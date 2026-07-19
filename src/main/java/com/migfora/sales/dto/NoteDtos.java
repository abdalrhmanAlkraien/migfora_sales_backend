package com.migfora.sales.dto;

import com.migfora.sales.entity.Note;
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
            Note.NoteType type,
            Long companyId,
            Long contactId,
            String content,
            String createdBy,
            String createdByName,
            boolean isOwner,            // ← new
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
