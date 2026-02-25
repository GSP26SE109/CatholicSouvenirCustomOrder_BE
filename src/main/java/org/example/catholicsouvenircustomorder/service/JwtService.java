package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.common.TokenClaims;
import org.example.catholicsouvenircustomorder.model.Account;

import java.util.UUID;

public interface JwtService {
    boolean decryptToken(String token);
    String getDataToken(String token);
    String generateToken(Account account);
    void invalidateToken(String token);
    boolean isTokenValid(String token);
    UUID getAccountIdFromToken(String token);
//    TokenClaims getAccountFromToken(String token);
}
