package com.fintracker.coreapi.financial.repository;

import com.fintracker.coreapi.financial.entity.LegacyUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LegacyUserMappingRepository extends JpaRepository<LegacyUserMapping, UUID> {
    Optional<LegacyUserMapping> findByProviderAndLegacyUserId(String provider, UUID legacyUserId);
    Optional<LegacyUserMapping> findByProviderAndAppUser_Id(String provider, UUID appUserId);
    boolean existsByProviderAndLegacyUserId(String provider, UUID legacyUserId);
}