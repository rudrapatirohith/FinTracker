package com.fintracker.coreapi.financial.service;

import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
import com.fintracker.coreapi.financial.dto.response.FinancialTrendRow;
import com.fintracker.coreapi.financial.repository.FinancialReportingRepository;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Applies reporting use-case rules before asking the repository for aggregates.
 *
 * <p>This is the boundary between the HTTP controller and SQL repository:
 * validate the requested dates, identify the logged-in owner, and delegate
 * the read operation. The caller never chooses a userId in the request.</p>
 */
@Service
public class FinancialReportingService {

    private final FinancialReportingRepository reportingRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public FinancialReportingService(
            FinancialReportingRepository reportingRepository,
            AuthenticatedUserProvider authenticatedUserProvider) {
        this.reportingRepository = reportingRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Returns grouped dashboard/report rows for the logged-in user.
     * Read-only transaction tells Spring this operation must not modify data.
     */
    @Transactional(readOnly = true)
    public List<FinancialReportRow> summarize(LocalDate from, LocalDate to) {
        validateRange(from, to);

        // Resolve identity from the security context; never trust a client-supplied owner ID.
        return reportingRepository.summarize(authenticatedUserProvider.getRequiredUserId(), from, to);
    }

    /**
     * Returns chart-ready monthly aggregates for the logged-in user.
     */
    @Transactional(readOnly = true)
    public List<FinancialTrendRow> monthlyTrends(LocalDate from, LocalDate to) {
        validateRange(from, to);

        // The same authenticated-owner rule applies to analytics as to dashboard reports.
        return reportingRepository.monthlyTrends(authenticatedUserProvider.getRequiredUserId(), from, to);
    }

    /**
     * Rejects invalid date ranges before any database query is executed.
     *
     * <p>Both boundaries are inclusive in the repository SQL. Therefore,
     * from == to is a valid single-day report, while from after to is invalid.
     * Future end dates are rejected because this API reports recorded history.</p>
     */
    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both from and to dates are required.");
        }
        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "The from date must be on or before the to date.");
        }
        if (to.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The to date cannot be in the future.");
        }
    }
}
