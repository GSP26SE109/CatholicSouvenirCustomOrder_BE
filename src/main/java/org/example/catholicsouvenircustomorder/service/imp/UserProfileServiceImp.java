package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.UpdateUserProfileRequest;
import org.example.catholicsouvenircustomorder.dto.response.UserProfileResponse;
import org.example.catholicsouvenircustomorder.exception.NotFoundException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.UserProfile;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.UserProfileRepository;
import org.example.catholicsouvenircustomorder.service.UserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImp implements UserProfileService {
    
    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;
    
    @Override
    @Transactional
    public UserProfileResponse createUserProfile(Account account) {
        log.info("Creating user profile for account: {}", account.getAccountId());
        
        // Check if profile already exists
        if (userProfileRepository.existsByAccount_AccountId(account.getAccountId())) {
            log.warn("User profile already exists for account: {}", account.getAccountId());
            return getUserProfile(account.getAccountId());
        }
        
        // Create new profile with data from account
        UserProfile profile = UserProfile.builder()
                .account(account)
                .fullName(account.getFullName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .gender(account.getGender())
                .dateOfBirth(account.getDateOfBirth())
                .avatarUrl(account.getAvt_url())
                .language("vi")
                .build();
        
        profile = userProfileRepository.save(profile);
        log.info("User profile created successfully: {}", profile.getProfileId());
        
        return mapToResponse(profile);
    }
    
    @Override
    public UserProfileResponse getUserProfile(UUID accountId) {
        log.info("Getting user profile for account: {}", accountId);
        
        UserProfile profile = userProfileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new NotFoundException("User profile not found for account: " + accountId));
        
        return mapToResponse(profile);
    }
    
    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(UUID accountId, UpdateUserProfileRequest request) {
        log.info("Updating user profile for account: {}", accountId);
        
        UserProfile profile = userProfileRepository.findByAccount_AccountId(accountId)
                .orElseThrow(() -> new NotFoundException("User profile not found for account: " + accountId));
        
        // Update fields if provided
        if (request.getFullName() != null) {
            profile.setFullName(request.getFullName());
            // Also update account
            Account account = profile.getAccount();
            account.setFullName(request.getFullName());
            accountRepository.save(account);
        }
        
        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone());
            Account account = profile.getAccount();
            account.setPhone(request.getPhone());
            accountRepository.save(account);
        }
        
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
            Account account = profile.getAccount();
            account.setGender(request.getGender());
            accountRepository.save(account);
        }
        
        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
            Account account = profile.getAccount();
            account.setDateOfBirth(request.getDateOfBirth());
            accountRepository.save(account);
        }
        
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
            Account account = profile.getAccount();
            account.setAvt_url(request.getAvatarUrl());
            accountRepository.save(account);
        }
        
        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getDistrict() != null) profile.setDistrict(request.getDistrict());
        if (request.getWard() != null) profile.setWard(request.getWard());
        if (request.getPostalCode() != null) profile.setPostalCode(request.getPostalCode());
        if (request.getSaintName() != null) profile.setSaintName(request.getSaintName());
        if (request.getLanguage() != null) profile.setLanguage(request.getLanguage());
        if (request.getTimezone() != null) profile.setTimezone(request.getTimezone());
        
        profile = userProfileRepository.save(profile);
        log.info("User profile updated successfully: {}", profile.getProfileId());
        
        return mapToResponse(profile);
    }
    
    @Override
    @Transactional
    public void deleteUserProfile(UUID accountId) {
        log.info("Deleting user profile for account: {}", accountId);
        
        if (!userProfileRepository.existsByAccount_AccountId(accountId)) {
            throw new NotFoundException("User profile not found for account: " + accountId);
        }
        
        userProfileRepository.deleteByAccount_AccountId(accountId);
        log.info("User profile deleted successfully for account: {}", accountId);
    }
    
    @Override
    @Transactional
    public void syncProfileFromAccount(Account account) {
        log.info("Syncing profile from account: {}", account.getAccountId());
        
        UserProfile profile = userProfileRepository.findByAccount_AccountId(account.getAccountId())
                .orElse(null);
        
        if (profile == null) {
            log.warn("Profile not found for account: {}, skipping sync", account.getAccountId());
            return;
        }
        
        // Sync basic fields from account
        profile.setFullName(account.getFullName());
        profile.setEmail(account.getEmail());
        profile.setPhone(account.getPhone());
        profile.setGender(account.getGender());
        profile.setDateOfBirth(account.getDateOfBirth());
        profile.setAvatarUrl(account.getAvt_url());
        
        userProfileRepository.save(profile);
        log.info("Profile synced successfully for account: {}", account.getAccountId());
    }
    
    /**
     * Map UserProfile entity to UserProfileResponse DTO
     */
    private UserProfileResponse mapToResponse(UserProfile profile) {
        Account account = profile.getAccount();
        
        return UserProfileResponse.builder()
                .profileId(profile.getProfileId())
                .accountId(account.getAccountId())
                .fullName(profile.getFullName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .gender(profile.getGender())
                .dateOfBirth(profile.getDateOfBirth())
                .avatarUrl(profile.getAvatarUrl())
                .bio(profile.getBio())
                .address(profile.getAddress())
                .city(profile.getCity())
                .district(profile.getDistrict())
                .ward(profile.getWard())
                .postalCode(profile.getPostalCode())
                .saintName(profile.getSaintName())
                .language(profile.getLanguage())
                .timezone(profile.getTimezone())
                .roleName(account.getRole() != null ? account.getRole().getName() : null)
                .isVerified(account.isVerified())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
