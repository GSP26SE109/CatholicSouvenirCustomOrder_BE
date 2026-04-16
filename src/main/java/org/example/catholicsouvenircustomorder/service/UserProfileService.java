package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.UpdateUserProfileRequest;
import org.example.catholicsouvenircustomorder.dto.response.UserProfileResponse;
import org.example.catholicsouvenircustomorder.model.Account;

import java.util.UUID;

/**
 * Service interface for managing user profiles
 */
public interface UserProfileService {
    
    /**
     * Create user profile when account is created and verified
     * @param account The account entity
     * @return UserProfileResponse
     */
    UserProfileResponse createUserProfile(Account account);
    
    /**
     * Get user profile by account ID
     * @param accountId The account UUID
     * @return UserProfileResponse
     * @throws org.example.catholicsouvenircustomorder.exception.NotFoundException if profile not found
     */
    UserProfileResponse getUserProfile(UUID accountId);
    
    /**
     * Update user profile
     * @param accountId The account UUID
     * @param request Update request with new profile data
     * @return Updated UserProfileResponse
     * @throws org.example.catholicsouvenircustomorder.exception.NotFoundException if profile not found
     */
    UserProfileResponse updateUserProfile(UUID accountId, UpdateUserProfileRequest request);
    
    /**
     * Delete user profile
     * @param accountId The account UUID
     * @throws org.example.catholicsouvenircustomorder.exception.NotFoundException if profile not found
     */
    void deleteUserProfile(UUID accountId);
    
    /**
     * Sync profile data from account (called when account is updated)
     * @param account The updated account entity
     */
    void syncProfileFromAccount(Account account);
}
