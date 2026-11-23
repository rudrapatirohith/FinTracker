package com.fintracker.coreapi.financial.controller;

import com.fintracker.coreapi.financial.dto.response.FinancialReportRow;
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
 * HTTP endpoints for dashboard summaries and category-level reporting.
 * Every request is authenticated by the API security chain; owner identity
 * is derived in the service and is never accepted from query parameters.
 */
@RestController
@Validated
public class FinancialReportingController {

    private final FinancialReportingService reportingService;

    public FinancialReportingController(FinancialReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/api/v1/dashboard/summary")
    public List<FinancialReportRow> dashboardSummary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }

    @GetMapping("/api/v1/analytics/categories")
    public List<FinancialReportRow> categoryAnalytics(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }

    @GetMapping("/api/v1/reports/summary")
    public List<FinancialReportRow> reportSummary(
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportingService.summarize(from, to);
    }
}
