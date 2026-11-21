package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.ScheduledPayment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, UUID> {
    /**
     * Returns only records belonging to the supplied owner UUID, with pagination applied in the database rather than loading the full collection.
     */
    Page<ScheduledPayment> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Finds a record only when both its primary key and owner UUID match; an empty result covers both missing and foreign-owned IDs.
     */
    Optional<ScheduledPayment> findByIdAndUserId(UUID id, UUID userId);
}
