package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.LegacyUserMapping;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyUserMappingRepository extends JpaRepository<LegacyUserMapping, UUID> {
    /**
     * Finds the mapping for the given identity provider and legacy-system user UUID.
     */
    Optional<LegacyUserMapping> findByProviderAndLegacyUserId(String provider, UUID legacyUserId);

    /**
     * Finds the mapping for the given identity provider and application-user UUID.
     */
    Optional<LegacyUserMapping> findByProviderAndAppUser_Id(String provider, UUID appUserId);

    /**
     * Checks whether the provider/legacy-user pair already has a mapping before a migration or linking operation.
     */
    boolean existsByProviderAndLegacyUserId(String provider, UUID legacyUserId);
}
