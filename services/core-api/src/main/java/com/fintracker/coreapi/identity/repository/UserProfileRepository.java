package com.fintracker.coreapi.identity.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fintracker.coreapi.identity.entity.UserProfile;

public interface UserProfileRepository
        extends JpaRepository<UserProfile, UUID> {
}