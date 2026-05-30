package com.migfora.sales.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:30 PM
 */
@NoArgsConstructor
public final class AuthDtos {

    // ------------- Request ---------------
    public record RegisterRequest(

            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password
    ) {
    }

    public record LoginRequest(

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {
    }

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserResponse user
    ) {
        public static AuthResponse of(String accessToken, String refreshToken,
                                      long expiresIn, UserResponse user) {
            return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
        }
    }

    public record UserResponse(
            Long id,
            String username,
            String email,
            Set<String> roles
    ) {
    }

    public record MessageResponse(String message) {
    }
}
