package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.ScheduledPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, UUID> {
    Page<ScheduledPayment> findAllByUserId(UUID userId, Pageable pageable);
    Optional<ScheduledPayment> findByIdAndUserId(UUID id, UUID userId);
}