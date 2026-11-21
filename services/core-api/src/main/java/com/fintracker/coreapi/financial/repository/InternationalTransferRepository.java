package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.InternationalTransfer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternationalTransferRepository extends JpaRepository<InternationalTransfer, UUID> {
    Page<InternationalTransfer> findAllByUserId(UUID userId, Pageable pageable);

    Optional<InternationalTransfer> findByIdAndUserId(UUID id, UUID userId);
}
