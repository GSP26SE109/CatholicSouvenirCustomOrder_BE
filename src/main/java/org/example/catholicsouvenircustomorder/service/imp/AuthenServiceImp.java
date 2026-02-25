package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.LoginRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterRequest;
import org.example.catholicsouvenircustomorder.dto.response.LoginResponse;
import org.example.catholicsouvenircustomorder.dto.response.RegisterResponse;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Role;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.RoleRepository;
import org.example.catholicsouvenircustomorder.service.AuthenService;
import org.example.catholicsouvenircustomorder.service.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenServiceImp implements AuthenService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        account.setVerified(true);
        
        Account savedAccount = accountRepository.save(account);
        
        return RegisterResponse.builder()
                .accountId(savedAccount.getAccountId())
                .fullName(savedAccount.getFullName())
                .email(savedAccount.getEmail())
                .phone(savedAccount.getPhone())
                .message("Đăng ký thành công")
                .build();
    }

    @Override
    public String login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }

        if (!account.isVerified()) {
            throw new RuntimeException("Tài khoản chưa được xác thực");
        }

        return jwtService.generateToken(account);
    }

    @Override
    public void logout(String token) {
        jwtService.invalidateToken(token);
    }
}
