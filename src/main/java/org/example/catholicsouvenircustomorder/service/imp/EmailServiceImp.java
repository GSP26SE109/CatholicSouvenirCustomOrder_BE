package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImp implements EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Override
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verifyUrl = baseUrl + "/api/authen/verify?token=" + verificationToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Xác thực tài khoản - Catholic Souvenir");
        message.setText(
            "Chào bạn,\n\n" +
            "Cảm ơn bạn đã đăng ký tài khoản tại Catholic Souvenir.\n\n" +
            "Vui lòng click vào link dưới đây để xác thực tài khoản:\n" +
            verifyUrl + "\n\n" +
            "Link này sẽ hết hạn sau 24 giờ.\n\n" +
            "Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.\n\n" +
            "Trân trọng,\n" +
            "Catholic Souvenir Team"
        );
        
        try {
            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email xác thực");
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = baseUrl + "/reset-password?token=" + resetToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu - Catholic Souvenir");
        message.setText(
            "Chào bạn,\n\n" +
            "Bạn đã yêu cầu đặt lại mật khẩu.\n\n" +
            "Vui lòng click vào link dưới đây để đặt lại mật khẩu:\n" +
            resetUrl + "\n\n" +
            "Link này sẽ hết hạn sau 1 giờ.\n\n" +
            "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
            "Trân trọng,\n" +
            "Catholic Souvenir Team"
        );
        
        try {
            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu");
        }
    }
}
