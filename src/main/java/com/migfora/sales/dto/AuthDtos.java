package com.migfora.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:30 PM
 */
@NoArgsConstructor
public final class AuthDtos {

    // ── Requests ──────────────────────────────────────────────────────────────

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {
    }

    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Email String email,
            @NotBlank String temporaryPassword,
            @NotBlank String newPassword,
            @NotBlank String session
    ) {
    }

    // ── Responses ─────────────────────────────────────────────────────────────

    public record UserResponse(
            String sub,
            String email,
            String name,
            String familyName,
            List<String> groups,
            boolean isAdmin
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String idToken,
            String tokenType,
            Integer expiresIn,
            UserResponse user
    ) {
        public static AuthResponse fromCognito(AuthenticationResultType result,
                                               UserResponse user) {
            return new AuthResponse(
                    result.accessToken(),
                    result.refreshToken(),
                    result.idToken(),
                    result.tokenType(),
                    result.expiresIn(),
                    user
            );
        }
    }

    public record MeResponse(
            String sub,
            String email,
            String name,
            List<String> groups
    ) {
    }

    public record MessageResponse(String message) {
    }
}
