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

public record TransferRequest(@NotBlank @Size(max=255) String recipientName,@NotBlank @Size(max=255) String recipientCountry,
 @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amountSent,@NotBlank @Pattern(regexp="INR|USD|EUR|GBP|CAD|AUD") String currencySent,
 @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amountReceived,@NotBlank @Pattern(regexp="INR|USD|EUR|GBP|CAD|AUD") String currencyReceived,
 @NotNull @DecimalMin("0.000001") @Digits(integer=4,fraction=6) BigDecimal exchangeRate,@DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal transferFee,
 @Size(max=100) String transferMethod,@Size(max=2000) String purpose,@NotBlank @Size(max=50) String status,@NotNull LocalDate transferDate,LocalDate completionDate,
 @Size(max=255) String referenceNumber,@DecimalMin("0.000001") @Digits(integer=4,fraction=6) BigDecimal usdToInrRate,@DecimalMin("0.00") @Digits(integer=13,fraction=2) BigDecimal inrEquivalent) {}
