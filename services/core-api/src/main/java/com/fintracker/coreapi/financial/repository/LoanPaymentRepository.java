package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.LoanPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, UUID> {
    Page<LoanPayment> findAllByUserId(UUID userId, Pageable pageable);
    Optional<LoanPayment> findByIdAndUserId(UUID id, UUID userId);
    Page<LoanPayment> findAllByLoanIdAndUserId(UUID loanId, UUID userId, Pageable pageable);
}