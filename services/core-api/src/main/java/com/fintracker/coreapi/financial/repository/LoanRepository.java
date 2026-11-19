package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    Page<Loan> findAllByUserId(UUID userId, Pageable pageable);
    Optional<Loan> findByIdAndUserId(UUID id, UUID userId);
}