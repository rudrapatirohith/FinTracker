package com.fintracker.coreapi.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fintracker.coreapi.ai.dto.AiChatRequest;
import com.fintracker.coreapi.financial.service.FinancialReportingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class AiAssistantServiceTest {

    @Test
    void refusesAiRequestsWhenServerCredentialIsNotConfigured() {
        FinancialReportingService reportingService = mock(FinancialReportingService.class);
        AiAssistantService service =
                new AiAssistantService(RestClient.builder(), reportingService, " ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.chat(new AiChatRequest("How much did I spend?", List.of())));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        verifyNoInteractions(reportingService);
    }
}
