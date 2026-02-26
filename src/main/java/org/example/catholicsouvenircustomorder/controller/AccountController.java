package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.CreateAccountRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateAccountRequest;
import org.example.catholicsouvenircustomorder.dto.response.AccountResponse;
import org.example.catholicsouvenircustomorder.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<BaseResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.ok(BaseResponse.success("Tạo tài khoản thành công", response));
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<BaseResponse> updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateAccountRequest request) {
        AccountResponse response = accountService.updateAccount(accountId, request);
        return ResponseEntity.ok(BaseResponse.success("Cập nhật tài khoản thành công", response));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<BaseResponse> getAccountById(@PathVariable UUID accountId) {
        AccountResponse response = accountService.getAccountById(accountId);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin tài khoản thành công", response));
    }

    @GetMapping
    public ResponseEntity<BaseResponse> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<AccountResponse> accounts = accountService.getAllAccounts(pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách tài khoản thành công", accounts));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<BaseResponse> deleteAccount(@PathVariable UUID accountId) {
        accountService.deleteAccount(accountId);
        return ResponseEntity.ok(BaseResponse.success("Xóa tài khoản thành công"));
    }

    @GetMapping("/search")
    public ResponseEntity<BaseResponse> searchAccountsByEmail(
            @RequestParam String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountResponse> accounts = accountService.searchAccountsByEmail(email, pageable);
        return ResponseEntity.ok(BaseResponse.success("Tìm kiếm tài khoản thành công", accounts));
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<BaseResponse> getAccountsByRole(
            @PathVariable int roleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountResponse> accounts = accountService.getAccountsByRole(roleId, pageable);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách tài khoản theo role thành công", accounts));
    }
}
