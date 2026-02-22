package org.example.catholicsouvenircustomorder.service.imp;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.service.AccountService;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Service
public class AccountServiceImp implements AccountService {
    private AccountRepository accountRepository;
    @Override
    public Account findAccountById(UUID id) {
        return accountRepository.findById(id).orElse(null);
    }
}
