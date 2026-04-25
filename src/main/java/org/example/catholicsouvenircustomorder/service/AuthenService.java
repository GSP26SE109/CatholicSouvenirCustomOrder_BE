package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.LoginRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterRequest;
import org.example.catholicsouvenircustomorder.dto.response.AuthenResponse;
import org.example.catholicsouvenircustomorder.dto.response.RegisterResponse;

public interface AuthenService {
    RegisterResponse register(RegisterRequest request);
    AuthenResponse login(LoginRequest request);
    void logout(String token);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    void forgotPassword(String email);
    void resetPassword(String token, String email);
}
