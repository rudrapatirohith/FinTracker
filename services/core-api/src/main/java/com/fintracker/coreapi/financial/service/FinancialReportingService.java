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
 * Coordinates the work needed to produce a safe financial report.
 *
 * <p>Think of this class as the report coordinator. Before asking the database
 * for numbers, it checks that the requested date range makes sense and identifies
 * the account that is currently signed in. It then asks the repository to fetch
 * the report for that account.</p>
 *
 * <p>It intentionally does not contain SQL or add financial amounts itself.
 * Keeping validation and identity checks here means every reporting endpoint
 * follows the same rules. The browser never supplies the owner ID; the server
 * gets it from the authenticated security context.</p>
 */
@Service
public class FinancialReportingService {

    private final FinancialReportingRepository reportingRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public FinancialReportingService(
            FinancialReportingRepository reportingRepository, AuthenticatedUserProvider authenticatedUserProvider) {
        this.reportingRepository = reportingRepository;
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    /**
     * Produces the shared dashboard/report summary for the signed-in user.
     *
     * <p>First the date range is checked. Then the signed-in user's ID is read
     * from Spring Security and passed to the repository with the two dates.
     * The read-only transaction marks this operation as a database read, not
     * a request to create or change financial records.</p>
     */
    @Transactional(readOnly = true)
    public List<FinancialReportRow> summarize(LocalDate from, LocalDate to) {
        validateRange(from, to);

        // Resolve identity from the security context; never trust a client-supplied owner ID.
        return reportingRepository.summarize(authenticatedUserProvider.getRequiredUserId(), from, to);
    }

    /**
     * Produces chart-ready totals grouped by calendar month for the signed-in user.
     * Date validation and owner lookup are identical to the summary operation,
     * while the repository uses its monthly grouping query for the chart result.
     */
    @Transactional(readOnly = true)
    public List<FinancialTrendRow> monthlyTrends(LocalDate from, LocalDate to) {
        validateRange(from, to);

        // The same authenticated-owner rule applies to analytics as to dashboard reports.
        return reportingRepository.monthlyTrends(authenticatedUserProvider.getRequiredUserId(), from, to);
    }

    /**
     * Produces chart-ready totals grouped by Monday-based calendar week. Identity
     * and date validation are shared with the monthly reporting operation.
     */
    @Transactional(readOnly = true)
    public List<FinancialTrendRow> weeklyTrends(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return reportingRepository.weeklyTrends(authenticatedUserProvider.getRequiredUserId(), from, to);
    }

    /**
     * Checks the date range before the application spends time querying PostgreSQL.
     *
     * <p>Both dates are required. The start date must be on or before the end date.
     * A range where both dates are the same is valid and means one day. The SQL
     * includes both boundary dates. Future end dates are rejected because these
     * endpoints currently report recorded history rather than forecasts.</p>
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
