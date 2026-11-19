package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.FinancialTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {
    Page<FinancialTransaction> findAllByUserId(UUID userId, Pageable pageable);
    Optional<FinancialTransaction> findByIdAndUserId(UUID id, UUID userId);
}