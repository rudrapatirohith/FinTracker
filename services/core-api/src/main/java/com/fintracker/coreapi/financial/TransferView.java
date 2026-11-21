package com.fintracker.coreapi.financial;

import com.fintracker.coreapi.financial.entity.*;
import com.fintracker.coreapi.financial.repository.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Public response projection for an international transfer. The mapper explicitly selects the transfer and conversion fields returned to clients while keeping persistence and owner metadata private.
 */
public record TransferView(
        UUID id,
        String recipientName,
        String recipientCountry,
        BigDecimal amountSent,
        String currencySent,
        BigDecimal amountReceived,
        String currencyReceived,
        BigDecimal exchangeRate,
        BigDecimal transferFee,
        String transferMethod,
        String purpose,
        String status,
        LocalDate transferDate,
        LocalDate completionDate,
        String referenceNumber,
        BigDecimal usdToInrRate,
        BigDecimal inrEquivalent) {
    static TransferView from(InternationalTransfer e) {
        return new TransferView(
                e.getId(),
                e.getRecipientName(),
                e.getRecipientCountry(),
                e.getAmountSent(),
                e.getCurrencySent(),
                e.getAmountReceived(),
                e.getCurrencyReceived(),
                e.getExchangeRate(),
                e.getTransferFee(),
                e.getTransferMethod(),
                e.getPurpose(),
                e.getStatus(),
                e.getTransferDate(),
                e.getCompletionDate(),
                e.getReferenceNumber(),
                e.getUsdToInrRate(),
                e.getInrEquivalent());
    }
}
