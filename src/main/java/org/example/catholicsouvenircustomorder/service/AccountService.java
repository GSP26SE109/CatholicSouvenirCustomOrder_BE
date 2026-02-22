package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.Account;

import java.util.UUID;

public interface AccountService {
    Account  findAccountById(UUID id);
}
