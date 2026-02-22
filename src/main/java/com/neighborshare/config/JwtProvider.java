package com.neighborshare.config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(UUID userId, UUID apartmentId, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("apartmentId", apartmentId.toString());
        claims.put("roles", roles);

        return Jwts.builder()
            .claims(claims)
            .subject(userId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs * 1000))
            .signWith(getSigningKey())
            .compact();
    }

    public String generateRefreshToken(UUID userId, UUID apartmentId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("apartmentId", apartmentId.toString());
        claims.put("type", "refresh");

        return Jwts.builder()
            .claims(claims)
            .subject(userId.toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs * 1000))
            .signWith(getSigningKey())
            .compact();
    }

    public UUID extractUserIdFromToken(String token) {
        try {
            String userIdStr = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
            return UUID.fromString(userIdStr);
        } catch (JwtException e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    public UUID extractApartmentIdFromToken(String token) {
        try {
            String apartmentIdStr = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("apartmentId", String.class);
            return UUID.fromString(apartmentIdStr);
        } catch (JwtException e) {
            log.warn("Failed to extract apartment ID from token: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}
