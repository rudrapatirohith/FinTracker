package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.Income;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IncomeRepository extends JpaRepository<Income, UUID> {
    Page<Income> findAllByUserId(UUID userId, Pageable pageable);
    Optional<Income> findByIdAndUserId(UUID id, UUID userId);
}