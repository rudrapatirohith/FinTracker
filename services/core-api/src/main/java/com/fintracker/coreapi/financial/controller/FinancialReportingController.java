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
 * HTTP endpoints for dashboard summaries, category analytics, and monthly
 * reporting. The authenticated owner is always resolved by the service.
 */
@RestController
@Validated
@RequestMapping("/api/v1")
public class FinancialReportingController {

    private final FinancialReportingService reportingService;

    public FinancialReportingController(FinancialReportingService reportingService) {
        this.reportingService = reportingService;
    }

    /** Returns owner-scoped financial metrics grouped by metric, category, and currency. */
    @GetMapping("/dashboard/summary")
    public List<FinancialReportRow> dashboardSummary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }

    /** Returns category-level income and outflow aggregates without mixing currencies. */
    @GetMapping("/analytics/categories")
    public List<FinancialReportRow> categoryAnalytics(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }

    /** Returns monthly trend rows suitable for income, expense, transfer, and payment charts. */
    @GetMapping("/analytics/monthly-trends")
    public List<FinancialTrendRow> monthlyTrends(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.monthlyTrends(from, to);
    }

    /** Returns the same currency-safe aggregates as a date-bounded financial report. */
    @GetMapping("/reports/summary")
    public List<FinancialReportRow> reportSummary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }
}
