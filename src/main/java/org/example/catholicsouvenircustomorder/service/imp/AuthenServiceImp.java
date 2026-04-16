package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.LoginRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterRequest;
import org.example.catholicsouvenircustomorder.dto.response.AuthenResponse;
import org.example.catholicsouvenircustomorder.dto.response.LoginResponse;
import org.example.catholicsouvenircustomorder.dto.response.RegisterResponse;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Role;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.RoleRepository;
import org.example.catholicsouvenircustomorder.service.AuthenService;
import org.example.catholicsouvenircustomorder.service.EmailService;
import org.example.catholicsouvenircustomorder.service.JwtService;
import org.example.catholicsouvenircustomorder.service.UserProfileService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenServiceImp implements AuthenService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final UserProfileService userProfileService;

    @Override
    public RegisterResponse register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }
        

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        

        Role userRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));

        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();

        Account account = new Account();
        account.setFullName(request.getFirstName() + " " + request.getLastName());
        account.setEmail(request.getEmail());
        String encryptedPassword = passwordEncoder.encode(request.getPassword());
        account.setPassword(encryptedPassword);
        account.setPhone(request.getPhoneNumber());
        account.setGender(request.getGender());
        account.setDateOfBirth(request.getDateOfBirth());
        account.setRole(userRole);
        account.setCreatedDate(LocalDateTime.now());
        account.setVerificationToken(verificationToken);
        account.setVerified(false);  // Set to false, require verification
        
        Account savedAccount = accountRepository.save(account);
        
        // Send verification email
        try {
            emailService.sendVerificationEmail(savedAccount.getEmail(), verificationToken);
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Failed to send verification email: " + e.getMessage());
        }
        
        return RegisterResponse.builder()
                .accountId(savedAccount.getAccountId())
                .fullName(savedAccount.getFullName())
                .email(savedAccount.getEmail())
                .phone(savedAccount.getPhone())
                .message("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.")
                .build();
    }

    @Override
    public AuthenResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }

        if (!account.isVerified()) {
            throw new RuntimeException("Tài khoản chưa được xác thực. Vui lòng kiểm tra email.");
        }

        String token = jwtService.generateToken(account);
        return AuthenResponse.builder()
                .accountId(account.getAccountId())
                .email(account.getEmail())
                .roleName(account.getRole().getName())
                .token(token)
                .build();
    }

    @Override
    public void logout(String token) {
        jwtService.invalidateToken(token);
    }
    
    @Override
    public void verifyEmail(String token) {
        Account account = accountRepository.findByVerificationToken(token)
                .orElseThrow(() -> new RuntimeException("Token xác thực không hợp lệ"));
        
        if (account.isVerified()) {
            throw new RuntimeException("Tài khoản đã được xác thực trước đó");
        }
        
        account.setVerified(true);
        account.setVerificationToken(null);  // Clear token after verification
        accountRepository.save(account);
        
        // Automatically create UserProfile after email verification
        try {
            userProfileService.createUserProfile(account);
        } catch (Exception e) {
            // Log error but don't fail verification
            System.err.println("Failed to create user profile: " + e.getMessage());
        }
    }
    
    @Override
    public void resendVerificationEmail(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này"));
        
        if (account.isVerified()) {
            throw new RuntimeException("Tài khoản đã được xác thực");
        }
        
        // Generate new token
        String newToken = UUID.randomUUID().toString();
        account.setVerificationToken(newToken);
        accountRepository.save(account);
        
        // Send email
        emailService.sendVerificationEmail(account.getEmail(), newToken);
    }
}
