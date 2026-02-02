package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.LoginRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterRequest;
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
        String token = authenService.login(request);
        return ResponseEntity.ok(BaseResponse.success("Đăng nhập thành công", token));
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
}
