package com.fintracker.coreapi.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
import com.fintracker.coreapi.financial.dto.response.FinancialTrendRow;
import com.fintracker.coreapi.financial.repository.FinancialReportingRepository;
import com.fintracker.coreapi.financial.service.FinancialReportingService;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for the reporting service. The repository is mocked so these tests
 * focus on service behavior: date validation, current-user lookup, and delegation.
 * Real SQL behavior is covered by FinancialReportingRepositoryPostgresIntegrationTests.
 */
@ExtendWith(MockitoExtension.class)
class FinancialReportingServiceTests {

    @Mock
    private FinancialReportingRepository reportingRepository;

    private final AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider();

    private FinancialReportingService reportingService;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        // Build an authenticated test principal. The service must derive this ID,
        // rather than accepting an owner ID from the HTTP request.
        ownerId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(ownerId.toString())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(jwt, "test-credentials", "ROLE_USER"));
        reportingService = new FinancialReportingService(reportingRepository, authenticatedUserProvider);
    }

    @AfterEach
    void clearSecurityContext() {
        // Avoid leaking the authenticated test principal into another test.
        SecurityContextHolder.clearContext();
    }

    @Test
    void summarizeUsesAuthenticatedOwnerAndReturnsCurrencyScopedRows() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        List<FinancialReportRow> expected = List.of(new FinancialReportRow(
                "INCOME", "Salary", "USD", new BigDecimal("2500.00"), 1L));
        when(reportingRepository.summarize(ownerId, from, to)).thenReturn(expected);

        assertThat(reportingService.summarize(from, to)).containsExactlyElementsOf(expected);
        // Verify that the ID from the authenticated principal is passed to the repository.
        verify(reportingRepository).summarize(ownerId, from, to);
    }

    @Test
    void monthlyTrendsUsesAuthenticatedOwnerAndReturnsTrendRows() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        List<FinancialTrendRow> expected = List.of(new FinancialTrendRow(
                "2026-01", "INCOME", "USD", new BigDecimal("2500.00"), 1L));
        when(reportingRepository.monthlyTrends(ownerId, from, to)).thenReturn(expected);

        assertThat(reportingService.monthlyTrends(from, to)).containsExactlyElementsOf(expected);
        verify(reportingRepository).monthlyTrends(ownerId, from, to);
    }

    @Test
    void summarizeRejectsMissingDatesAsBadRequest() {
        // Missing either boundary should fail before the repository is called.
        assertThatThrownBy(() -> reportingService.summarize(null, LocalDate.now()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void summarizeRejectsInvertedDateRangeAsBadRequest() {
        // A start date after the end date is not a meaningful reporting interval.
        assertThatThrownBy(() -> reportingService.summarize(
                        LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void summarizeRejectsFutureEndDateAsBadRequest() {
        // This API reports recorded history, so the requested end date cannot be in the future.
        assertThatThrownBy(() -> reportingService.summarize(
                        LocalDate.now(), LocalDate.now().plusDays(1)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
