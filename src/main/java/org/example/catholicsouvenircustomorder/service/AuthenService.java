package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.LoginRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterRequest;
import org.example.catholicsouvenircustomorder.dto.response.RegisterResponse;

public interface AuthenService {
    RegisterResponse register(RegisterRequest request);
    String login(LoginRequest request);
    void logout(String token);
}
