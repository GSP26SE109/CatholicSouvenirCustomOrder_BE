package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.CreateAccountRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateAccountRequest;
import org.example.catholicsouvenircustomorder.dto.response.AccountResponse;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Role;
import org.example.catholicsouvenircustomorder.model.Saint;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.RoleRepository;
import org.example.catholicsouvenircustomorder.repository.SaintRepository;
import org.example.catholicsouvenircustomorder.service.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImp implements AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final SaintRepository saintRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Account findAccountById(UUID id) {
        return accountRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));

        Account account = new Account();
        account.setFullName(request.getFullName());
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setPhone(request.getPhone());
        account.setGender(request.getGender());
        account.setDateOfBirth(request.getDateOfBirth());
        account.setAvt_url(request.getAvtUrl());
        account.setRole(role);
        account.setVerified(request.getIsVerified());
        account.setCreated_date(LocalDateTime.now());

        if (request.getSaintId() != null) {
            Saint saint = saintRepository.findById(request.getSaintId())
                    .orElseThrow(() -> new RuntimeException("Saint không tồn tại"));
            account.setSaint(saint);
        }

        Account savedAccount = accountRepository.save(account);
        return mapToResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(UUID accountId, UpdateAccountRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (request.getFullName() != null) {
            account.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(account.getEmail())) {
            if (accountRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng");
            }
            account.setEmail(request.getEmail());
        }

        if (request.getPhone() != null) {
            account.setPhone(request.getPhone());
        }

        if (request.getGender() != null) {
            account.setGender(request.getGender());
        }

        if (request.getDateOfBirth() != null) {
            account.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getAvtUrl() != null) {
            account.setAvt_url(request.getAvtUrl());
        }

        if (request.getRoleId() != 0) {
            Role role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
            account.setRole(role);
        }

        if (request.getSaintId() != null) {
            Saint saint = saintRepository.findById(request.getSaintId())
                    .orElseThrow(() -> new RuntimeException("Saint không tồn tại"));
            account.setSaint(saint);
        }

        if (request.getIsVerified() != null) {
            account.setVerified(request.getIsVerified());
        }

        account.setUpdated_date(LocalDateTime.now());

        Account updatedAccount = accountRepository.save(account);
        return mapToResponse(updatedAccount);
    }

    @Override
    public AccountResponse getAccountById(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return mapToResponse(account);
    }

    @Override
    public Page<AccountResponse> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        accountRepository.delete(account);
    }

    @Override
    public Page<AccountResponse> searchAccountsByEmail(String email, Pageable pageable) {
        return accountRepository.findByEmailContainingIgnoreCase(email, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AccountResponse> getAccountsByRole(int roleId, Pageable pageable) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại"));
        return accountRepository.findByRole(role, pageable)
                .map(this::mapToResponse);
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .fullName(account.getFullName())
                .email(account.getEmail())
                .phone(account.getPhone())
                .gender(account.getGender())
                .dateOfBirth(account.getDateOfBirth())
                .avtUrl(account.getAvt_url())
                .isVerified(account.isVerified())
                .createdDate(account.getCreated_date())
                .updatedDate(account.getUpdated_date())
                .roleName(account.getRole() != null ? account.getRole().getName() : null)
                .roleId(account.getRole() != null ? account.getRole().getRoleId() : null)
                .saintName(account.getSaint() != null ? account.getSaint().getSaintName() : null)
                .saintId(account.getSaint() != null ? account.getSaint().getSaintId() : null)
                .build();
    }
}
