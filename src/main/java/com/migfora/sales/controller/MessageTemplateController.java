package com.migfora.sales.controller;

import com.migfora.sales.dto.MessageTemplateDtos.*;
import com.migfora.sales.entity.MessageTemplate;
import com.migfora.sales.service.MessageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:47 PM
 */
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Tag(name = "Message Templates", description = "Sales outreach message templates")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class MessageTemplateController {

    private final MessageTemplateService templateService;

    @Operation(summary = "Create template")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(
            @Valid @RequestBody CreateTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return templateService.create(request, jwt.getSubject());
    }

    @Operation(summary = "List templates with filters")
    @GetMapping
    public Page<TemplateListResponse> getAll(
            @RequestParam(required = false) MessageTemplate.TemplateType type,
            @RequestParam(required = false) MessageTemplate.DoorType doorType,
            @RequestParam(required = false) MessageTemplate.TemplateChannel channel,
            @RequestParam(required = false) MessageTemplate.TemplateLanguage language,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return templateService.getAll(
                type, doorType, channel, language, activeOnly, search, pageable);
    }

    @Operation(summary = "Get template by ID")
    @GetMapping("/{id}")
    public TemplateResponse getById(@PathVariable Long id) {
        return templateService.getById(id);
    }

    @Operation(summary = "Update template")
    @PatchMapping("/{id}")
    public TemplateResponse update(
            @PathVariable Long id,
            @RequestBody UpdateTemplateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return templateService.update(id, request, jwt.getSubject());
    }

    @Operation(summary = "Mark template as used — updates lastUsedAt")
    @PostMapping("/{id}/used")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsUsed(@PathVariable Long id) {
        templateService.markAsUsed(id);
    }

    @Operation(summary = "Delete template — admin only")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        templateService.delete(id, jwt.getSubject());
    }
}