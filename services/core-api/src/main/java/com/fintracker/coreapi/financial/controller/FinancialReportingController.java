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
 * HTTP entry points for dashboard, category analytics, monthly trends, and reports.
 *
 * <p>The controller's responsibility is deliberately small: parse the HTTP
 * request, require the date parameters, and delegate business rules to the
 * service. It does not run SQL or decide which user owns the data.</p>
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
     * Dashboard totals for the requested date range.
     * Example: GET /api/v1/dashboard/summary?from=2026-01-01&to=2026-01-31
     */
    @GetMapping("/dashboard/summary")
    public List<FinancialReportRow> dashboardSummary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }

    /**
     * Category breakdown for the requested date range.
     * The repository groups each result by category as well as metric and currency.
     */
    @GetMapping("/analytics/categories")
    public List<FinancialReportRow> categoryAnalytics(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }

    /**
     * Monthly chart data. Each result represents one month, metric, and currency.
     * Example period value: 2026-01.
     */
    @GetMapping("/analytics/monthly-trends")
    public List<FinancialTrendRow> monthlyTrends(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.monthlyTrends(from, to);
    }

    /**
     * Date-bounded financial report. Uses the same currency-safe aggregate model
     * as the dashboard, so reporting clients receive consistent definitions.
     */
    @GetMapping("/reports/summary")
    public List<FinancialReportRow> reportSummary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }
}
