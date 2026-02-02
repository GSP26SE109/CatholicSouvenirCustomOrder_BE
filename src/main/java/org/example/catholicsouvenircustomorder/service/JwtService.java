package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.Account;

public interface JwtService {
    boolean decryptToken(String token);
    String getDataToken(String token);
    String generateToken(Account account);
    void invalidateToken(String token);
    boolean isTokenValid(String token);
    Integer getAccountIdFromToken(String token);
}
