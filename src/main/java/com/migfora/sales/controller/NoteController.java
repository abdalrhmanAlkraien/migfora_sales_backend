package com.migfora.sales.controller;

import com.migfora.sales.dto.NoteDtos.*;
import com.migfora.sales.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/07/2026
 * @Time: 12:57 PM
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Company notes")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class NoteController {

    private final NoteService noteService;

    @Operation(summary = "Add note to company")
    @PostMapping("/api/v1/companies/{companyId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateNoteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return noteService.create(companyId, request, jwt.getSubject());
    }

    @Operation(summary = "Add multiple notes to a company")
    @PostMapping("/api/v1/companies/{companyId}/notes/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<NoteResponse> createBulk(
            @PathVariable Long companyId,
            @RequestBody @Size(min = 1, message = "At least one note is required")
            List<@Valid CreateNoteRequest> requests,
            @AuthenticationPrincipal Jwt jwt) {
        return noteService.createBulkApi(companyId, requests, jwt.getSubject());
    }

    @Operation(summary = "Get all notes for a company")
    @GetMapping("/api/v1/companies/{companyId}/notes")
    public Page<NoteResponse> getByCompany(
            @PathVariable Long companyId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return noteService.getByCompany(companyId, pageable);
    }

    @Operation(summary = "Update note")
    @PatchMapping("/api/v1/notes/{id}")
    public NoteResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return noteService.update(id, request, jwt.getSubject());
    }

    @Operation(summary = "Delete note")
    @DeleteMapping("/api/v1/notes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        noteService.delete(id, jwt.getSubject());
    }
}
