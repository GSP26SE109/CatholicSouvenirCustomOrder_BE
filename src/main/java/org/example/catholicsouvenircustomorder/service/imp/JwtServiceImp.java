package org.example.catholicsouvenircustomorder.service.imp;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Service
public class JwtServiceImp implements JwtService {

    @Value("${jwt.key}")
    private String keyJWT;
    
    @Value("${jwt.expiration:86400000}") // 24 hours default
    private long jwtExpiration;
    
    // Store invalidated tokens (in production, use Redis)
    private final Set<String> blacklistedTokens = new HashSet<>();

    @Override
    public boolean decryptToken(String token) {
        if (blacklistedTokens.contains(token)) {
            return false;
        }
        
        boolean result = false;
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(keyJWT));
        try {
            Jwts.parser().verifyWith(key).build().parseClaimsJws(token);
            result = true;
        } catch (Exception e) {
            System.out.println("Decrypt token error : " + e.getMessage());
        }
        return result;
    }

    @Override
    public String getDataToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(keyJWT));
        String email = "";
        try {
            email = Jwts.parser().verifyWith(key).build()
                    .parseClaimsJws(token).getPayload().getSubject();
        } catch (Exception e) {
            System.out.println("Decrypt token error : " + e.getMessage());
        }
        return email;
    }
    
    @Override
    public String generateToken(Account account) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(keyJWT));
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .subject(account.getEmail())
                .claim("accountId", account.getAccountId().toString())
                .claim("role", account.getRole().getName())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
    
    @Override
    public void invalidateToken(String token) {
        blacklistedTokens.add(token);
    }

    @Override
    public boolean isTokenValid(String token) {
        if(token == null || token.trim().isEmpty()) {
            return false;
        }

        return !blacklistedTokens.contains(token);
    }

    @Override
    public Integer getAccountIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(keyJWT));
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseClaimsJws(token)
                    .getPayload()
                    .get("accountId", Integer.class);
        } catch (Exception e) {
            System.out.println("Get accountId from token error: " + e.getMessage());
            return null;
        }
    }


}
