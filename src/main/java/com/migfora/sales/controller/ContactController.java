package com.migfora.sales.controller;

import com.migfora.sales.dto.ContactDtos.*;
import com.migfora.sales.service.ContactService;
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
 * @Date: 31/05/2026
 * @Time: 3:26 PM
 */
@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Tag(name = "Contacts", description = "Contact management per company")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class ContactController {


    private final ContactService contactService;

    @Operation(summary = "Get all contacts for a company")
    @GetMapping("/companies/{companyId}")
    public Page<ContactResponse> getByCompany(
            @PathVariable Long companyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return contactService.getByCompany(companyId, search, pageable);
    }

    @Operation(summary = "Create a contact for a company")
    @PostMapping("/companies/{companyId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse create(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return contactService.create(companyId, request, jwt.getSubject());
    }

    @Operation(summary = "Get contact by ID")
    @GetMapping("/{id}")
    public ContactResponse getById(@PathVariable Long id) {
        return contactService.getById(id);
    }

    @Operation(summary = "Update contact")
    @PatchMapping("/{id}")
    public ContactResponse update(
            @PathVariable Long id,
            @RequestBody UpdateContactRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return contactService.update(id, request, jwt.getSubject());
    }

    @Operation(summary = "Quick status update")
    @PatchMapping("/{id}/status")
    public ContactResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContactStatusRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return contactService.updateStatus(id, request, jwt.getSubject());
    }

    @Operation(summary = "Delete contact")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        contactService.delete(id, jwt.getSubject());
    }

    // ── Follow-ups ────────────────────────────────────────────────────────────

    @Operation(summary = "Get all follow-ups for a contact")
    @GetMapping("/{contactId}/followups")
    public Page<FollowUpResponse> getFollowUps(
            @PathVariable Long contactId,
            @PageableDefault(size = 10) Pageable pageable) {
        return contactService.getFollowUps(contactId, pageable);
    }

    @Operation(summary = "Create a new follow-up for a contact")
    @PostMapping("/{contactId}/followups")
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpResponse createFollowUp(
            @PathVariable Long contactId,
            @Valid @RequestBody CreateFollowUpRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return contactService.createFollowUp(contactId, request, jwt.getSubject());
    }

    @Operation(summary = "Get follow-up by ID")
    @GetMapping("/followups/{id}")
    public FollowUpResponse getFollowUpById(@PathVariable Long id) {
        return contactService.getFollowUpById(id);
    }

    @Operation(summary = "Update follow-up")
    @PatchMapping("/followups/{id}")
    public FollowUpResponse updateFollowUp(
            @PathVariable Long id,
            @RequestBody UpdateFollowUpRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return contactService.updateFollowUp(id, request, jwt.getSubject());
    }

    @Operation(summary = "Delete follow-up")
    @DeleteMapping("/followups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFollowUp(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        contactService.deleteFollowUp(id, jwt.getSubject());
    }
}
