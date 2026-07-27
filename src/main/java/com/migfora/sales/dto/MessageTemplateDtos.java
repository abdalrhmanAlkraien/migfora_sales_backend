package com.migfora.sales.dto;

import com.migfora.sales.entity.MessageTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:46 PM
 */
public class MessageTemplateDtos {

    public record CreateTemplateRequest(
            @NotBlank String title,
            String subject,
            @NotBlank String content,
            @NotNull MessageTemplate.TemplateType type,
            @NotNull MessageTemplate.DoorType doorType,
            @NotNull MessageTemplate.TemplateChannel channel,
            MessageTemplate.TemplateLanguage language,
            String tags
    ) {}

    public record UpdateTemplateRequest(
            String title,
            String subject,
            String content,
            MessageTemplate.TemplateType type,
            MessageTemplate.DoorType doorType,
            MessageTemplate.TemplateChannel channel,
            MessageTemplate.TemplateLanguage language,
            String tags,
            Boolean active
    ) {}

    public record TemplateResponse(
            Long id,
            String title,
            String subject,
            String content,
            MessageTemplate.TemplateType type,
            MessageTemplate.DoorType doorType,
            MessageTemplate.TemplateChannel channel,
            MessageTemplate.TemplateLanguage language,
            String tags,
            boolean active,
            LocalDateTime lastUsedAt,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record TemplateListResponse(
            Long id,
            String title,
            String subject,
            MessageTemplate.TemplateType type,
            MessageTemplate.DoorType doorType,
            MessageTemplate.TemplateChannel channel,
            MessageTemplate.TemplateLanguage language,
            String tags,
            boolean active,
            LocalDateTime lastUsedAt,
            LocalDateTime createdAt
    ) {}
}