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

/** Application service for owner-scoped dashboard, analytics, and reporting read models. */
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

    @Transactional(readOnly = true)
    public List<FinancialReportRow> summarize(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return reportingRepository.summarize(authenticatedUserProvider.getRequiredUserId(), from, to);
    }

    @Transactional(readOnly = true)
    public List<FinancialTrendRow> monthlyTrends(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return reportingRepository.monthlyTrends(authenticatedUserProvider.getRequiredUserId(), from, to);
    }

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
