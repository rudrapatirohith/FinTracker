package com.fintracker.coreapi.identity.repository;

import com.fintracker.coreapi.identity.entity.UserProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {}
