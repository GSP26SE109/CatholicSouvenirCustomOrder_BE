package org.example.catholicsouvenircustomorder.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.repository.PasswordResetTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled task to cleanup expired password reset tokens
 * Runs daily at 2 AM to remove expired tokens from database
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {
    
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    
    /**
     * Delete expired password reset tokens
     * Runs every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired password reset tokens");
        
        try {
            passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Successfully cleaned up expired password reset tokens");
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
        }
    }
}
