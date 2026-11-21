/**
 * Persistence queries for income records.
 * Collection reads require userId and return a Page so the database limits the result set; single-record reads require both the record ID and user ID, making tenant ownership part of the SQL predicate rather than a check performed after loading the row.
 */
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