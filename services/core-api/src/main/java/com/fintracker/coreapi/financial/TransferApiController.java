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

@RestController @RequestMapping("/api/v1/international-transfers")
public class TransferApiController {
 private final TransferApiService service; TransferApiController(TransferApiService s){service=s;}
 @PostMapping ResponseEntity<TransferView> create(@Valid @RequestBody TransferRequest r){return ResponseEntity.status(201).body(service.create(r));}
 @GetMapping Page<TransferView> all(@PageableDefault(size=20,sort="transferDate",direction=Sort.Direction.DESC) Pageable p){return service.all(p);}
 @GetMapping("/{id}") TransferView one(@PathVariable UUID id){return service.one(id);}
 @PutMapping("/{id}") TransferView update(@PathVariable UUID id,@Valid @RequestBody TransferRequest r){return service.update(id,r);}
 @DeleteMapping("/{id}") ResponseEntity<Void> delete(@PathVariable UUID id){service.delete(id);return ResponseEntity.noContent().build();}
}
