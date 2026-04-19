package org.example.catholicsouvenircustomorder.service.imp;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class JwtServiceImp implements JwtService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.key}")
    private String keyJWT;

    @Value("${jwt.expiration:86400000}") // 24 hours default
    private long jwtExpiration;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    @Override
    public boolean decryptToken(String token) {
        // Check if token is blacklisted in Redis
        if (Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token))) {
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
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("role", String.class);

        }catch (Exception e) {
            System.out.println("Decrypt token error : " + e.getMessage());
            return null;
        }

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
        // Add token to Redis blacklist with expiration time
        // Set expiration to match JWT expiration time
        redisTemplate.opsForValue().set(
            BLACKLIST_PREFIX + token, 
            "blacklisted", 
            jwtExpiration, 
            TimeUnit.MILLISECONDS
        );
    }

    @Override
    public boolean isTokenValid(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // Check if token is NOT in blacklist
        return !Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    @Override
    public UUID getAccountIdFromToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(keyJWT));
        try {
            return UUID.fromString(
                    Jwts.parser()
                            .verifyWith(key)
                            .build()
                            .parseSignedClaims(token)
                            .getPayload()
                            .get("accountId", String.class)
            );
        } catch (Exception e) {
            System.out.println("Get accountId from token error: " + e.getMessage());
            return null;
        }
    }


}
