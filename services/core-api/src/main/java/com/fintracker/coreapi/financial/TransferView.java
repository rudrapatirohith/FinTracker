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

public record TransferView(UUID id,String recipientName,String recipientCountry,BigDecimal amountSent,String currencySent,BigDecimal amountReceived,String currencyReceived,BigDecimal exchangeRate,BigDecimal transferFee,String transferMethod,String purpose,String status,LocalDate transferDate,LocalDate completionDate,String referenceNumber,BigDecimal usdToInrRate,BigDecimal inrEquivalent) {
 static TransferView from(InternationalTransfer e){return new TransferView(e.getId(),e.getRecipientName(),e.getRecipientCountry(),e.getAmountSent(),e.getCurrencySent(),e.getAmountReceived(),e.getCurrencyReceived(),e.getExchangeRate(),e.getTransferFee(),e.getTransferMethod(),e.getPurpose(),e.getStatus(),e.getTransferDate(),e.getCompletionDate(),e.getReferenceNumber(),e.getUsdToInrRate(),e.getInrEquivalent());}
}
