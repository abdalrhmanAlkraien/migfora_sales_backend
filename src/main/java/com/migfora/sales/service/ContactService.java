package com.migfora.sales.service;

import com.migfora.sales.dto.ContactDtos.*;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Contact;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:24 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;


    @Transactional
    public ContactResponse create(CreateContactRequest request, String createdBy) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new AuthException("Company not found."));

        Contact contact = Contact.builder()
                .name(request.name())
                .title(request.title())
                .email(request.email())
                .phone(request.phone())
                .linkedIn(request.linkedIn())
                .notes(request.notes())
                .company(company)
                .createdBy(createdBy)
                .build();

        Contact saved = contactRepository.save(contact);
        log.info("Contact created | id={} company={} by={}", saved.getId(), company.getId(), createdBy);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> getByCompany(Long companyId,
                                              String search,
                                              Pageable pageable) {
        Pageable unsorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        return contactRepository.searchByCompany(companyId, search, unsorted)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ContactResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ContactResponse update(Long id, UpdateContactRequest request, String updatedBy) {
        Contact contact = findById(id);

        if (request.name()     != null) contact.setName(request.name());
        if (request.title()    != null) contact.setTitle(request.title());
        if (request.email()    != null) contact.setEmail(request.email());
        if (request.phone()    != null) contact.setPhone(request.phone());
        if (request.linkedIn() != null) contact.setLinkedIn(request.linkedIn());
        if (request.notes()    != null) contact.setNotes(request.notes());
        if (request.status()   != null) contact.setStatus(request.status());

        log.info("Contact updated | id={} by={}", id, updatedBy);
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        contactRepository.deleteById(id);
        log.info("Contact deleted | id={} by={}", id, deletedBy);
    }

    private Contact findById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new AuthException("Contact not found."));
    }

    private ContactResponse toResponse(Contact c) {
        return new ContactResponse(
                c.getId(), c.getName(), c.getTitle(),
                c.getEmail(), c.getPhone(), c.getLinkedIn(),
                c.getNotes(), c.getStatus(),
                c.getCompany().getId(), c.getCompany().getName(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
