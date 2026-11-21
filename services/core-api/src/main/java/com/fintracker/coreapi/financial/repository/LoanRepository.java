/**
 * Persistence queries for loan records.
 * User-scoped pagination prevents one user's loans from appearing in another user's collection, while the composite ID-and-user lookup supports safe detail, update, and delete operations.
 */
package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.Loan;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    Page<Loan> findAllByUserId(UUID userId, Pageable pageable);

    Optional<Loan> findByIdAndUserId(UUID id, UUID userId);
}
