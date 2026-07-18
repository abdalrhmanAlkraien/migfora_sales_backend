package com.migfora.sales.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/07/2026
 * @Time: 12:55 PM
 */
public class NoteDtos {

    public record CreateNoteRequest(
            @NotBlank String content
    ) {}

    public record UpdateNoteRequest(
            @NotBlank String content
    ) {}

    public record NoteResponse(
            Long id,
            Long companyId,
            String companyName,
            String content,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
