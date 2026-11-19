package com.fintracker.coreapi.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import com.fintracker.coreapi.identity.service.AuthenticatedUserProvider;

public record LoanRequest(@NotBlank @Size(max=255) String loanName,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal principalAmount,
 @NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal currentBalance,@DecimalMin("0.00") @Digits(integer=3,fraction=2) BigDecimal interestRate,
 @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal monthlyPayment,@NotNull LocalDate startDate,LocalDate endDate,@Size(max=255) String lender,
 @Size(max=100) String loanType,@NotBlank @Pattern(regexp="active|paid|closed|defaulted") String status,@NotBlank @Pattern(regexp="INR|USD") String currency) {}
