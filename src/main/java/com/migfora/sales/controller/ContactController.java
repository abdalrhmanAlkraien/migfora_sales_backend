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

    @Operation(summary = "Create a contact for a company")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse create(
            @Valid @RequestBody CreateContactRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return contactService.create(request, jwt.getSubject());
    }

    @Operation(summary = "Get all contacts for a company")
    @GetMapping("/company/{companyId}")
    public Page<ContactResponse> getByCompany(
            @PathVariable Long companyId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return contactService.getByCompany(companyId, search, pageable);
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

    @Operation(summary = "Delete contact")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        contactService.delete(id, jwt.getSubject());
    }
}
