package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.ForgotPasswordRequest;
import org.example.catholicsouvenircustomorder.dto.request.LoginRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterRequest;
import org.example.catholicsouvenircustomorder.dto.request.ResetPasswordRequest;
import org.example.catholicsouvenircustomorder.dto.response.AuthenResponse;
import org.example.catholicsouvenircustomorder.dto.response.RegisterResponse;
import org.example.catholicsouvenircustomorder.exception.InvalidTokenException;
import org.example.catholicsouvenircustomorder.service.AuthenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/authen")
@RequiredArgsConstructor
public class AuthenController {

    @Autowired
    private AuthenService authenService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authenService.register(request);
        return ResponseEntity.ok(BaseResponse.success("Đăng ký thành công", response));
    }

    @PostMapping("/login")
    public ResponseEntity<BaseResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthenResponse response = authenService.login(request);
        return ResponseEntity.ok(BaseResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("Token không hợp lệ");
        }

        String token = authHeader.substring(7);
        authenService.logout(token);
        return ResponseEntity.ok(BaseResponse.success("Đăng xuất thành công"));
    }
    
    @GetMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            authenService.verifyEmail(token);
            // Redirect về trang login của FE với status success
            String redirectUrl = "https://catholic-souvenir-custom-order-fe.vercel.app/login?verified=success";
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        } catch (Exception e) {
            // Redirect về trang login với status error
            String redirectUrl = "https://catholic-souvenir-custom-order-fe.vercel.app/login?verified=error&message=" + e.getMessage();
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();
        }
    }
    
    @PostMapping("/resend-verification")
    public ResponseEntity<BaseResponse> resendVerificationEmail(@RequestParam String email) {
        authenService.resendVerificationEmail(email);
        return ResponseEntity.ok(BaseResponse.success("Email xác thực đã được gửi lại. Vui lòng kiểm tra hộp thư."));
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<BaseResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authenService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(BaseResponse.success("Email đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư."));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok(BaseResponse.success("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới."));
    }
    
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, org.springframework.ui.Model model) {
        try {
            // Validate token exists and not expired
            authenService.validateResetToken(token);
            model.addAttribute("token", token);
            return "reset-password-form";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "reset-password-form";
        }
    }
    
    @PostMapping("/reset-password-submit")
    public String submitResetPassword(@RequestParam String token,
                                     @RequestParam String newPassword,
                                     @RequestParam String confirmPassword,
                                     org.springframework.ui.Model model) {
        try {
            authenService.resetPassword(token, newPassword, confirmPassword);
            model.addAttribute("success", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới.");
            return "reset-password-form";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("token", token);
            return "reset-password-form";
        }
    }
}
