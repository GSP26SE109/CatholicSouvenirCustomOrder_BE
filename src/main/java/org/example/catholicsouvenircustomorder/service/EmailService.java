package org.example.catholicsouvenircustomorder.service;

public interface EmailService {
    void sendVerificationEmail(String toEmail, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String resetToken);
}
