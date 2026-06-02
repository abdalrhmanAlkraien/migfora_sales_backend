package com.migfora.sales.dto;

import com.migfora.sales.entity.Contact.*;
import com.migfora.sales.entity.FollowUp.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:22 PM
 */
@NoArgsConstructor
public class ContactDtos {

    // ── Contact Requests ──────────────────────────────────────────────────────

    public record CreateContactRequest(
            @NotBlank String name,
            String title,
            String email,
            String phone,
            String linkedIn,
            String notes,
            ContactStatus status          // optional — defaults to NEW
    ) {}

    public record UpdateContactRequest(
            String name,
            String title,
            String email,
            String phone,
            String linkedIn,
            String notes,
            ContactStatus status
    ) {}

    public record UpdateContactStatusRequest(
            @NotNull ContactStatus status
    ) {}

    // ── Contact Response ──────────────────────────────────────────────────────

    public record ContactResponse(
            Long id,
            String name,
            String title,
            String email,
            String phone,
            String linkedIn,
            String notes,
            ContactStatus status,
            Long companyId,
            String companyName,
            String createdBy,
            long followUpsCount,
            long pendingFollowUpsCount,
            LocalDateTime lastFollowUpAt,
            LocalDateTime nextFollowUpAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    // ── Follow-up Requests ────────────────────────────────────────────────────

    public record CreateFollowUpRequest(
            @NotNull FollowUpType type,
            @NotNull LocalDateTime scheduledAt,
            String notes
    ) {}

    public record UpdateFollowUpRequest(
            FollowUpType type,
            FollowUpStatus status,
            LocalDateTime scheduledAt,
            LocalDateTime completedAt,
            String notes,
            String outcome
    ) {}

    // ── Follow-up Response ────────────────────────────────────────────────────

    public record FollowUpResponse(
            Long id,
            Long contactId,
            String contactName,
            Long companyId,
            String companyName,
            FollowUpType type,
            FollowUpStatus status,
            LocalDateTime scheduledAt,
            LocalDateTime completedAt,
            String notes,
            String outcome,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
