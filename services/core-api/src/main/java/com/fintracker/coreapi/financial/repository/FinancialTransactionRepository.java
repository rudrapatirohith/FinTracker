package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.FinancialTransaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, UUID> {
    /**
     * Returns only records belonging to the supplied owner UUID, with pagination applied in the database rather than loading the full collection.
     */
    Page<FinancialTransaction> findAllByUserId(UUID userId, Pageable pageable);

    /**
     * Finds a record only when both its primary key and owner UUID match; an empty result covers both missing and foreign-owned IDs.
     */
    Optional<FinancialTransaction> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Checks whether a reference UUID identifies one of the canonical financial event rows
     * owned by the same user. Generic transaction history rows may mirror these records,
     * but a reference to another user's row or an unknown UUID is rejected before saving.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM income WHERE id = :referenceId AND user_id = :userId
                UNION ALL
                SELECT 1 FROM monthly_expenses WHERE id = :referenceId AND user_id = :userId
                UNION ALL
                SELECT 1 FROM loan_payments WHERE id = :referenceId AND user_id = :userId
                UNION ALL
                SELECT 1 FROM international_transfers WHERE id = :referenceId AND user_id = :userId
                UNION ALL
                SELECT 1 FROM scheduled_payments WHERE id = :referenceId AND user_id = :userId
            )
            """, nativeQuery = true)
    boolean existsCanonicalSourceForUser(@Param("referenceId") UUID referenceId, @Param("userId") UUID userId);
}
