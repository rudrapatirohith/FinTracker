package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.Loan;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    /**
     * Returns only records belonging to the supplied owner UUID, with pagination applied in the database rather than loading the full collection.
     */
    Page<Loan> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Finds a record only when both its primary key and owner UUID match; an empty result covers both missing and foreign-owned IDs.
     */
    Optional<Loan> findByIdAndUserId(UUID id, UUID userId);
}
