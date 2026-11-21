package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.MonthlyExpense;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyExpenseRepository extends JpaRepository<MonthlyExpense, UUID> {
    Page<MonthlyExpense> findAllByUserId(UUID userId, Pageable pageable);

    Optional<MonthlyExpense> findByIdAndUserId(UUID id, UUID userId);
}
