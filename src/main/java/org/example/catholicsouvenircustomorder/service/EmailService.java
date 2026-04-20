package org.example.catholicsouvenircustomorder.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String resetToken);
    void sendArtisanApplicationSubmittedEmail(String toEmail, String artisanName);
    void sendArtisanApplicationApprovedEmail(String toEmail, String artisanName);
    void sendArtisanApplicationRejectedEmail(String toEmail, String artisanName, String reason);
}
