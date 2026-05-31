package com.migfora.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.List;
import java.util.Set;

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
    ) {}

    public record RefreshTokenRequest(
            @NotBlank String refreshToken
    ) {}

    public record ChangePasswordRequest(
            @NotBlank @Email String email,
            @NotBlank String temporaryPassword,
            @NotBlank String newPassword,
            @NotBlank String session
    ) {}

    public record CreateUserRequest(
            @NotBlank @Email
            String email,

            @NotBlank
            String name,

            @NotBlank
            String familyName,

            @NotBlank
            @Pattern(regexp = "^\\+[1-9]\\d{6,14}$",
                    message = "Phone must be in E.164 format e.g. +962791234567")
            String phoneNumber,

            @NotBlank
            String role   // ADMIN or SALES
    ) {}

    // ── Responses ─────────────────────────────────────────────────────────────

    public record UserResponse(
            String sub,
            String email,
            String name,
            String familyName,
            List<String> groups,
            boolean isAdmin
    ) {}

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
    ) {}

    public record MessageResponse(String message) {}
}
