package com.migfora.sales.dto;

import com.migfora.sales.entity.Contact;
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

    public record CreateContactRequest(
            @NotBlank String name,
            String title,
            String email,
            String phone,
            String linkedIn,
            String notes,
            @NotNull Long companyId
    ) {}

    public record UpdateContactRequest(
            String name,
            String title,
            String email,
            String phone,
            String linkedIn,
            String notes,
            Contact.ContactStatus status
    ) {}

    public record ContactResponse(
            Long id,
            String name,
            String title,
            String email,
            String phone,
            String linkedIn,
            String notes,
            Contact.ContactStatus status,
            Long companyId,
            String companyName,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
