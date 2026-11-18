package com.fintracker.coreapi.identity.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fintracker.coreapi.identity.entity.OAuthIdentity;
import com.fintracker.coreapi.identity.entity.OAuthProvider;

public interface OAuthIdentityRepository
        extends JpaRepository<OAuthIdentity, UUID> {

    Optional<OAuthIdentity> findByProviderAndProviderSubject(
        OAuthProvider provider,
        String providerSubject
    );

    boolean existsByProviderAndProviderSubject(
        OAuthProvider provider,
        String providerSubject
    );
}