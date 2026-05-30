package com.migfora.sales.service;

import com.migfora.sales.enitty.RefreshToken;
import com.migfora.sales.enitty.User;
import com.migfora.sales.exception.TokenRefreshException;
import com.migfora.sales.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:39 PM
 */

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-days}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Revoke existing tokens (single-session model)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationMs))
                .build();

        return refreshTokenRepository.save(token);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token has expired. Please log in again.");
        }
        return token;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token,
                        "Refresh token not found."));
    }

    @Transactional
    public void revokeByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
