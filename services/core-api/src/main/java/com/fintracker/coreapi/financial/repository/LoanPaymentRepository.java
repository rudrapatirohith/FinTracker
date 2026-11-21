package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.LoanPayment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanPaymentRepository extends JpaRepository<LoanPayment, UUID> {
    /**
     * Returns only records belonging to the supplied owner UUID, with pagination applied in the database rather than loading the full collection.
     */
    Page<LoanPayment> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Finds a record only when both its primary key and owner UUID match; an empty result covers both missing and foreign-owned IDs.
     */
    Optional<LoanPayment> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Returns a page of payments filtered by both parent loan ID and owner ID, ensuring nested payment listings cannot cross either boundary.
     */
    Page<LoanPayment> findAllByLoanIdAndUserId(UUID loanId, UUID userId, Pageable pageable);
}
