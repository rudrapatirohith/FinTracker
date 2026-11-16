package com.fintracker.coreapi.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.fintracker.coreapi.identity.entity.UserSession;

// JpaRepository<UserSession, UUID> gives us built-in database methods 
// like save(), findById(), delete(), and count() for free.
public interface UserSessionRepository
        extends JpaRepository<UserSession, UUID> {

    // Spring Data reads this method name and automatically writes the SQL:
    // "SELECT * FROM user_sessions WHERE refresh_token_hash = ?"
    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    // Finds all active (non-revoked) login sessions for a specific user ID.
    // ("User_Id" means look inside the UserSession's 'user' object for its 'id',
    // and "RevokedAtIsNull" means the session hasn't been canceled yet.)
    List<UserSession> findAllByUser_IdAndRevokedAtIsNull(UUID userId);

    /**
     * STEP 1 OF ROTATION: FIND AND LOCK THE ROW
     * 
     * @Lock(LockModeType.PESSIMISTIC_WRITE) tells PostgreSQL:
     * "Put a 'Do Not Touch' lock on this specific database row until I finish."
     * 
     * Why? If your browser accidentally sends 2 refresh requests at the exact 
     * same millisecond, Request #2 has to wait in line until Request #1 finishes
     * marking the old token as used. This stops duplicate tokens from being created.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM UserSession s
        WHERE s.refreshTokenHash = :hash
        """)
    Optional<UserSession> findByRefreshTokenHashForUpdate(
        @Param("hash") String hash // Connects the Java 'hash' variable to ':hash' in the query above
    );

    /**
     * STEP 2 OF THEFT DETECTION: KILL THE ENTIRE TOKEN FAMILY
     * 
     * @Modifying tells Spring: "This query is UPDATING data, not just reading it."
     * 
     * When someone tries to reuse an old/revoked token, we pass its 'familyId' here.
     * This single SQL query finds EVERY active token belonging to that login session 
     * (WHERE s.tokenFamilyId = :familyId AND s.revokedAt IS NULL) and marks them all 
     * as revoked at once—locking out both the hacker and the user until next login.
     */
    @Modifying
    @Query("""
        UPDATE UserSession s
        SET s.revokedAt = :now,
            s.revocationReason = :reason
        WHERE s.tokenFamilyId = :familyId
        AND s.revokedAt IS NULL
        """)
    int revokeActiveByTokenFamilyId(
        @Param("familyId") UUID familyId, // The shared group ID for this login chain
        @Param("now") Instant now,        // The exact timestamp when we detected the theft
        @Param("reason") String reason    // e.g., "REUSE_DETECTED" for security logs
    );
}