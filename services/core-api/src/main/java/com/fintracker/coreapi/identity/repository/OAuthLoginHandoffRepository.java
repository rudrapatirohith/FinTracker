package com.fintracker.coreapi.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import com.fintracker.coreapi.identity.entity.OAuthLoginHandoff;

/**
 * Database operations for temporary OAuth login handoffs.
 */
@Repository
public interface OAuthLoginHandoffRepository
        extends JpaRepository<OAuthLoginHandoff, UUID> {

    /**
     * Locks a handoff row while it is being exchanged.
     *
     * This prevents two concurrent requests from successfully
     * consuming the same one-time authorization code.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT h
        FROM OAuthLoginHandoff h
        WHERE h.codeHash = :codeHash
        """)
    Optional<OAuthLoginHandoff> findByCodeHashForUpdate(
        @Param("codeHash") String codeHash
    );
}
