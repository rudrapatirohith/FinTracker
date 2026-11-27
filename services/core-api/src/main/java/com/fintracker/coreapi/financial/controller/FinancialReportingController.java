package com.fintracker.coreapi.financial.controller;

import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
import com.fintracker.coreapi.financial.dto.response.FinancialTrendRow;
import com.fintracker.coreapi.financial.service.FinancialReportingService;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives the frontend's requests for dashboard totals, category analytics,
 * monthly trends, and financial reports.
 *
 * <p>Think of this class as the reception desk: it accepts the requested report
 * and its date range, then hands the request to the service that knows how to
 * validate it and retrieve the correct user's data. It does not calculate money,
 * run SQL, or accept a user ID from the browser.</p>
 *
 * <p>Request journey: browser URL -> Spring converts the date text into
 * {@link LocalDate} values -> this controller calls {@link FinancialReportingService}
 * -> the service returns result objects -> Spring converts those objects to JSON.</p>
 *
 * <p>The dashboard, category, and report routes currently share the same summary
 * result to keep financial definitions consistent. They are separate URLs for
 * their different consumers, not separate calculations. If their response needs
 * diverge later, introduce a specific response model/use case rather than copying SQL.</p>
 */
@RestController
@Validated
@RequestMapping("/api/v1")
public class FinancialReportingController {

    private final FinancialReportingService reportingService;

    public FinancialReportingController(FinancialReportingService reportingService) {
        this.reportingService = reportingService;
    }

    /**
     * Returns the summary rows used by the dashboard for the requested dates.
     * Example: GET /api/v1/dashboard/summary?from=2026-01-01&to=2026-01-31.
     * The service handles validation and identifies the signed-in account.
     */
    @GetMapping("/dashboard/summary")
    public List<FinancialReportRow> dashboardSummary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }

    /**
     * Returns the shared summary rows for category-oriented screens.
     * Each row includes its category, financial metric, and currency. This route
     * currently delegates to the same calculation as the dashboard; it does not
     * run a second category-specific SQL query.
     */
    @GetMapping("/analytics/categories")
    public List<FinancialReportRow> categoryAnalytics(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }

    /**
     * Returns monthly chart data. Each result represents one month, metric, and
     * currency, for example January 2026 income in USD. The repository performs
     * the month grouping; this controller only forwards the requested date range.
     */
    @GetMapping("/analytics/monthly-trends")
    public List<FinancialTrendRow> monthlyTrends(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.monthlyTrends(from, to);
    }

    /**
     * Returns weekly activity totals for the selected date range. Each row keeps
     * metric and currency separate, matching the monthly trend contract.
     */
    @GetMapping("/analytics/weekly-trends")
    public List<FinancialTrendRow> weeklyTrends(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.weeklyTrends(from, to);
    }

    /**
     * Returns the shared currency-safe summary for report screens.
     * It deliberately reuses the dashboard calculation so the same financial
     * source is not calculated differently in two places.
     */
    @GetMapping("/reports/summary")
    public List<FinancialReportRow> reportSummary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }
}
