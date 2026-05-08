package org.example.catholicsouvenircustomorder.service.imp;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImp implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Override
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        String verifyUrl = baseUrl + "/api/authen/verify?token=" + verificationToken;
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản - Catholic Souvenir");
            
            Context context = new Context();
            context.setVariable("verifyUrl", verifyUrl);
            
            String htmlContent = templateEngine.process("email-verification", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email xác thực");
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = baseUrl + "/api/authen/reset-password?token=" + resetToken;
        
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Đặt lại mật khẩu - Catholic Souvenir");
            
            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);
            
            String htmlContent = templateEngine.process("email-password-reset", context);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Password reset email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Không thể gửi email đặt lại mật khẩu");
        }
    }
    
    @Override
    public void sendArtisanApplicationSubmittedEmail(String toEmail, String artisanName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Đơn đăng ký nghệ nhân đã được tiếp nhận - Catholic Souvenir");
            
            String htmlContent = String.format(
                "<html><body style='font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #2c3e50;'>Xin chào %s,</h2>" +
                "<p>Cảm ơn bạn đã đăng ký trở thành nghệ nhân trên nền tảng Catholic Souvenir.</p>" +
                "<p>Đơn đăng ký của bạn đã được tiếp nhận và đang chờ xét duyệt.</p>" +
                "<p><strong>Thời gian xét duyệt:</strong> Trong vòng 7 ngày làm việc</p>" +
                "<p>Chúng tôi sẽ gửi email thông báo kết quả xét duyệt đến bạn sớm nhất có thể.</p>" +
                "<p>Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi.</p>" +
                "<br>" +
                "<p>Trân trọng,<br>Đội ngũ Catholic Souvenir</p>" +
                "</body></html>",
                artisanName
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Artisan application submitted email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send artisan application submitted email to: {}", toEmail, e);
        }
    }
    
    @Override
    public void sendArtisanApplicationApprovedEmail(String toEmail, String artisanName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Chúc mừng! Đơn đăng ký nghệ nhân đã được phê duyệt - Catholic Souvenir");
            
            String htmlContent = String.format(
                "<html><body style='font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #27ae60;'>Xin chào %s,</h2>" +
                "<p><strong>Chúc mừng!</strong> Đơn đăng ký nghệ nhân của bạn đã được phê duyệt.</p>" +
                "<p>Bạn đã chính thức trở thành nghệ nhân trên nền tảng Catholic Souvenir.</p>" +
                "<p><strong>Bước tiếp theo:</strong></p>" +
                "<ul>" +
                "<li>Đăng nhập vào tài khoản của bạn</li>" +
                "<li>Cập nhật thông tin cá nhân và portfolio</li>" +
                "<li>Bắt đầu tạo sản phẩm và template</li>" +
                "<li>Nhận đơn hàng từ khách hàng</li>" +
                "</ul>" +
                "<p>Chúc bạn thành công và phát triển trên nền tảng của chúng tôi!</p>" +
                "<br>" +
                "<p>Trân trọng,<br>Đội ngũ Catholic Souvenir</p>" +
                "</body></html>",
                artisanName
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Artisan application approved email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send artisan application approved email to: {}", toEmail, e);
        }
    }
    
    @Override
    public void sendArtisanApplicationRejectedEmail(String toEmail, String artisanName, String reason) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Thông báo về đơn đăng ký nghệ nhân - Catholic Souvenir");
            
            String htmlContent = String.format(
                "<html><body style='font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #2c3e50;'>Xin chào %s,</h2>" +
                "<p>Cảm ơn bạn đã quan tâm và đăng ký trở thành nghệ nhân trên nền tảng Catholic Souvenir.</p>" +
                "<p>Rất tiếc, đơn đăng ký của bạn chưa được phê duyệt lần này.</p>" +
                "<p><strong>Lý do:</strong> %s</p>" +
                "<p>Bạn có thể nộp đơn đăng ký mới sau khi đã khắc phục các vấn đề trên.</p>" +
                "<p>Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi để được hỗ trợ.</p>" +
                "<br>" +
                "<p>Trân trọng,<br>Đội ngũ Catholic Souvenir</p>" +
                "</body></html>",
                artisanName,
                reason
            );
            
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Artisan application rejected email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send artisan application rejected email to: {}", toEmail, e);
        }
    }
}
