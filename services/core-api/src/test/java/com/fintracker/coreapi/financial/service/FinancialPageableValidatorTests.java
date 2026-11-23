package com.fintracker.coreapi.financial.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

class FinancialPageableValidatorTests {
    @Test
    void acceptsAnAllowlistedSortProperty() {
        assertThatCode(() -> FinancialPageableValidator.validate(
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "paymentDate")),
                        Set.of("paymentDate", "amount")))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAnUnapprovedEntityProperty() {
        assertThatThrownBy(() -> FinancialPageableValidator.validate(
                        PageRequest.of(0, 20, Sort.by("userId")), Set.of("paymentDate", "amount")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported sort property");
    }
}
