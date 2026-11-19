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

@Service public class TransferApiService {
 private final InternationalTransferRepository repo; private final AuthenticatedUserProvider users;
 TransferApiService(InternationalTransferRepository r,AuthenticatedUserProvider u){repo=r;users=u;}
 @Transactional TransferView create(TransferRequest r){UUID uid=users.getRequiredUserId();InternationalTransfer e=new InternationalTransfer(uid,r.recipientName().trim(),r.recipientCountry().trim(),r.amountSent(),r.amountReceived(),r.exchangeRate(),r.transferDate());apply(e,r);return TransferView.from(repo.save(e));}
 @Transactional(readOnly=true) Page<TransferView> all(Pageable p){return repo.findAllByUserId(users.getRequiredUserId(),p).map(TransferView::from);}
 @Transactional(readOnly=true) TransferView one(UUID id){return TransferView.from(owned(id));}
 @Transactional TransferView update(UUID id,TransferRequest r){InternationalTransfer e=owned(id);apply(e,r);return TransferView.from(e);}
 @Transactional void delete(UUID id){repo.delete(owned(id));}
 private InternationalTransfer owned(UUID id){return repo.findByIdAndUserId(id,users.getRequiredUserId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Transfer not found."));}
 private void apply(InternationalTransfer e,TransferRequest r){String a=r.currencySent().toUpperCase(Locale.ROOT),b=r.currencyReceived().toUpperCase(Locale.ROOT);e.updateDetails(r.recipientName().trim(),r.recipientCountry().trim(),r.amountSent(),a,r.amountReceived(),b,r.exchangeRate(),r.transferFee()==null?BigDecimal.ZERO:r.transferFee(),r.transferMethod(),r.purpose(),r.status().trim().toLowerCase(Locale.ROOT),r.transferDate(),r.completionDate(),r.referenceNumber(),r.usdToInrRate(),r.inrEquivalent());}
}
