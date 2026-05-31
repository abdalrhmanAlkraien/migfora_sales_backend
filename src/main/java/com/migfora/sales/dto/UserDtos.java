package com.migfora.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:37 PM
 */
public class UserDtos {


    private UserDtos() {}

    // ── Requests ──────────────────────────────────────────────────────────────

    public record UpdateUserRequest(
            String name,
            String familyName,

            @Pattern(
                    regexp = "^\\+[1-9]\\d{6,14}$",
                    message = "Phone must be E.164 format e.g. +962791234567"
            )
            String phoneNumber
    ) {}

    // ── Responses ─────────────────────────────────────────────────────────────

    public record UserDetailResponse(
            String sub,
            String email,
            String name,
            String familyName,
            String phoneNumber,
            boolean emailVerified,
            boolean phoneVerified,
            boolean enabled,
            String status,
            List<String> groups,
            boolean isAdmin,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record UserListResponse(
            List<UserDetailResponse> users,
            String nextToken,
            int total
    ) {}

    public record MessageResponse(String message) {}
}
