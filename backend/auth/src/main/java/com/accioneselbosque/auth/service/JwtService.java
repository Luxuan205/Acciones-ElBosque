package com.accioneselbosque.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.ttl-hours:24}")
    private int jwtTtlHours;

    private SecretKey secretKey;

    @PostConstruct
    private void initKey() {
        // IMP-4: Cache the HMAC key to avoid re-creating it on every token generation
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long investorId, String role) {
        return Jwts.builder()
                .subject(String.valueOf(investorId))
                .claim("roles", List.of(role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (long) jwtTtlHours * 3600_000L))
                .signWith(secretKey)
                .compact();
    }
}
