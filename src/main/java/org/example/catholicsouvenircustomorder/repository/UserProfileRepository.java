package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    
    /**
     * Find user profile by account ID
     * @param accountId The account UUID
     * @return Optional containing the user profile
     */
    Optional<UserProfile> findByAccount_AccountId(UUID accountId);
    
    /**
     * Check if user profile exists for an account
     * @param accountId The account UUID
     * @return true if exists, false otherwise
     */
    boolean existsByAccount_AccountId(UUID accountId);
    
    /**
     * Delete user profile by account ID
     * @param accountId The account UUID
     */
    void deleteByAccount_AccountId(UUID accountId);
}
