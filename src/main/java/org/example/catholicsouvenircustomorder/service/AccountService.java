package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.CreateAccountRequest;
import org.example.catholicsouvenircustomorder.dto.request.UpdateAccountRequest;
import org.example.catholicsouvenircustomorder.dto.response.AccountResponse;
import org.example.catholicsouvenircustomorder.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AccountService {
    AccountResponse createAccount(CreateAccountRequest request);
    AccountResponse updateAccount(UUID accountId, UpdateAccountRequest request);
    AccountResponse getAccountById(UUID accountId);
    Page<AccountResponse> getAllAccounts(Pageable pageable);
    void deleteAccount(UUID accountId);
    Page<AccountResponse> searchAccountsByEmail(String email, Pageable pageable);
    Page<AccountResponse> getAccountsByRole(int roleId, Pageable pageable);
    Account findAccountById(UUID id);
}
