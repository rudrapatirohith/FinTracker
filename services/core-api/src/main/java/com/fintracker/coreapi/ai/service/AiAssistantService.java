package com.fintracker.coreapi.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fintracker.coreapi.ai.dto.AiChatRequest;
import com.fintracker.coreapi.ai.dto.AiChatResponse;
import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
import com.fintracker.coreapi.financial.service.FinancialReportingService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Calls Gemini from the trusted backend boundary. Financial context is generated
 * from the current user's owner-scoped reporting query; the browser cannot forge
 * or substitute another user's financial summary.
 */
@Service
public class AiAssistantService {
    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);
    private static final String GEMINI_MODEL = "gemini-2.5-flash";

    private final RestClient restClient;
    private final FinancialReportingService reportingService;
    private final String apiKey;

    public AiAssistantService(
            RestClient.Builder restClientBuilder,
            FinancialReportingService reportingService,
            @Value("${fintracker.ai.gemini.api-key:}") String apiKey) {
        this.restClient = restClientBuilder.build();
        this.reportingService = reportingService;
        this.apiKey = apiKey;
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI assistant is not configured on the server.");
        }

        LocalDate today = LocalDate.now();
        LocalDate earliest = LocalDate.of(2000, 1, 1);
        List<FinancialReportRow> allTime = reportingService.summarize(earliest, today);
        LocalDate monthStart = YearMonth.now().atDay(1);
        List<FinancialReportRow> monthToDate = reportingService.summarize(monthStart, today);

        String systemInstruction = """
                You are FinTracker's personal finance assistant.
                Answer in concise, practical language. Use USD for displayed amounts when converting
                from the provided currency values is possible; otherwise preserve the source currency.
                Treat the financial summary as private user data. Do not claim to execute transactions,
                change account settings, or provide regulated investment, tax, or legal advice.
                If the data does not contain the answer, say what is missing instead of inventing values.

                ALL-TIME FINANCIAL SUMMARY:
                %s

                CURRENT-MONTH FINANCIAL SUMMARY:
                %s
                """.formatted(formatRows(allTime), formatRows(monthToDate));

        List<Map<String, Object>> contents = new ArrayList<>();
        if (request.history() != null) {
            for (AiChatRequest.ChatMessage message : request.history()) {
                contents.add(geminiMessage(
                        "assistant".equals(message.role()) ? "model" : "user",
                        message.content()));
            }
        }
        contents.add(geminiMessage("user", request.message()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        payload.put("contents", contents);
        payload.put("generationConfig", Map.of("temperature", 0.4, "maxOutputTokens", 700));

        try {
            JsonNode result = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("generativelanguage.googleapis.com")
                            .path("/v1beta/models/" + GEMINI_MODEL + ":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String answer = result == null
                    ? ""
                    : result.path("candidates")
                            .path(0)
                            .path("content")
                            .path("parts")
                            .path(0)
                            .path("text")
                            .asText("");

            if (answer.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "The AI provider returned an empty response.");
            }
            return new AiChatResponse(answer);
        } catch (RestClientResponseException exception) {
            // Never log the provider URL: the API key is a query parameter.
            log.warn("Gemini request failed with HTTP status {}", exception.getStatusCode().value());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "The AI provider could not complete the request.");
        }
    }

    private static Map<String, Object> geminiMessage(String role, String content) {
        return Map.of("role", role, "parts", List.of(Map.of("text", content)));
    }

    private static String formatRows(List<FinancialReportRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return "No recorded financial activity.";
        }
        return rows.stream()
                .map(row -> "%s | %s | %s %s | %d records".formatted(
                        row.metric(),
                        row.category() == null ? "Other" : row.category(),
                        row.totalAmount(),
                        row.currency(),
                        row.recordCount()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No recorded financial activity.");
    }
}
