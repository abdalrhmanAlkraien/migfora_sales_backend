package com.migfora.sales.service;

import com.migfora.sales.dto.ContactDtos.*;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Contact;
import com.migfora.sales.entity.Contact.*;
import com.migfora.sales.entity.FollowUp;
import com.migfora.sales.entity.FollowUp.*;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.repository.ContactRepository;
import com.migfora.sales.repository.FollowUpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

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
    private final FollowUpRepository followUpRepository;

    // ── Contacts ──────────────────────────────────────────────────────────────

    @Transactional
    public ContactResponse create(Long companyId,
                                  CreateContactRequest request,
                                  String createdBy) {
        var company = companyRepository.findById(companyId)
                .orElseThrow(() -> new AuthException("Company not found."));

        Contact contact = Contact.builder()
                .name(request.name())
                .title(request.title())
                .email(request.email())
                .phone(request.phone())
                .linkedIn(request.linkedIn())
                .notes(request.notes())
                .status(request.status() != null ? request.status() : ContactStatus.NEW)
                .company(company)
                .createdBy(createdBy)
                .build();

        Contact saved = contactRepository.save(contact);
        log.info("Contact created | id={} company={} by={}", saved.getId(), companyId, createdBy);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> getByCompany(Long companyId,
                                              String search,
                                              Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return contactRepository.searchByCompany(companyId, search, unsorted)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ContactResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ContactResponse update(Long id,
                                  UpdateContactRequest request,
                                  String updatedBy) {
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
    public ContactResponse updateStatus(Long id,
                                        UpdateContactStatusRequest request,
                                        String updatedBy) {
        Contact contact = findById(id);
        contact.setStatus(request.status());
        log.info("Contact status updated | id={} status={} by={}", id, request.status(), updatedBy);
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        contactRepository.deleteById(id);
        log.info("Contact deleted | id={} by={}", id, deletedBy);
    }

    // ── Follow-ups ────────────────────────────────────────────────────────────

    @Transactional
    public FollowUpResponse createFollowUp(Long contactId,
                                           CreateFollowUpRequest request,
                                           String createdBy) {
        Contact contact = findById(contactId);

        FollowUp followUp = FollowUp.builder()
                .contact(contact)
                .type(request.type())
                .scheduledAt(request.scheduledAt())
                .notes(request.notes())
                .status(FollowUpStatus.SCHEDULED)
                .createdBy(createdBy)
                .build();

        FollowUp saved = followUpRepository.save(followUp);

        // Recalculate contact's next follow-up date
        refreshFollowUpDates(contact);

        log.info("FollowUp created | id={} contact={} by={}",
                saved.getId(), contactId, createdBy);
        return toFollowUpResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<FollowUpResponse> getFollowUps(Long contactId, Pageable pageable) {
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return followUpRepository.findByContactId(contactId, unsorted)
                .map(this::toFollowUpResponse);
    }

    @Transactional(readOnly = true)
    public FollowUpResponse getFollowUpById(Long id) {
        return toFollowUpResponse(findFollowUpById(id));
    }

    @Transactional
    public FollowUpResponse updateFollowUp(Long id,
                                           UpdateFollowUpRequest request,
                                           String updatedBy) {
        FollowUp followUp = findFollowUpById(id);
        Contact contact = followUp.getContact();

        if (request.type()        != null) followUp.setType(request.type());
        if (request.status()      != null) {
            followUp.setStatus(request.status());
            if (request.status() == FollowUpStatus.DONE
                    && followUp.getCompletedAt() == null) {
                followUp.setCompletedAt(LocalDateTime.now());
            }
        }
        if (request.scheduledAt() != null) followUp.setScheduledAt(request.scheduledAt());
        if (request.completedAt() != null) followUp.setCompletedAt(request.completedAt());
        if (request.notes()       != null) followUp.setNotes(request.notes());
        if (request.outcome()     != null) followUp.setOutcome(request.outcome());

        FollowUp saved = followUpRepository.save(followUp);

        // Recalculate contact's next follow-up date
        refreshFollowUpDates(contact);

        log.info("FollowUp updated | id={} by={}", id, updatedBy);
        return toFollowUpResponse(saved);
    }

    @Transactional
    public void deleteFollowUp(Long id, String deletedBy) {
        FollowUp followUp = findFollowUpById(id);
        Contact contact = followUp.getContact();

        followUpRepository.deleteById(id);

        // Recalculate contact's next follow-up date
        refreshFollowUpDates(contact);

        log.info("FollowUp deleted | id={} by={}", id, deletedBy);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Contact findById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new AuthException("Contact not found."));
    }

    private FollowUp findFollowUpById(Long id) {
        return followUpRepository.findById(id)
                .orElseThrow(() -> new AuthException("Follow-up not found."));
    }

    public ContactResponse toResponse(Contact c) {
        long total   = followUpRepository.countByContactId(c.getId());
        long pending = followUpRepository
                .countByContactIdAndStatus(c.getId(), FollowUpStatus.SCHEDULED);

        return new ContactResponse(
                c.getId(), c.getName(), c.getTitle(),
                c.getEmail(), c.getPhone(), c.getLinkedIn(), c.getNotes(),
                c.getStatus(),
                c.getCompany().getId(), c.getCompany().getName(),
                c.getCreatedBy(),
                total, pending,
                c.getLastFollowUpAt(),   // ← from contact field
                c.getNextFollowUpAt(),   // ← from contact field
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    private FollowUpResponse toFollowUpResponse(FollowUp f) {
        return new FollowUpResponse(
                f.getId(),
                f.getContact().getId(),
                f.getContact().getName(),
                f.getContact().getCompany().getId(),
                f.getContact().getCompany().getName(),
                f.getType(), f.getStatus(),
                f.getScheduledAt(), f.getCompletedAt(),
                f.getNotes(), f.getOutcome() == null ? "" : f.getOutcome(),
                f.getCreatedBy(),
                f.getCreatedAt(), f.getUpdatedAt()
        );
    }

    private void refreshFollowUpDates(Contact contact) {
        // Next follow-up = earliest SCHEDULED follow-up from now
        Optional<FollowUp> next = followUpRepository
                .findFirstByContactIdAndStatusOrderByScheduledAtAsc(
                        contact.getId(), FollowUpStatus.SCHEDULED);

        // Last follow-up = most recent DONE follow-up
        Optional<FollowUp> last = followUpRepository
                .findFirstByContactIdAndStatusOrderByScheduledAtDesc(
                        contact.getId(), FollowUpStatus.DONE);

        contact.setNextFollowUpAt(next.map(FollowUp::getScheduledAt).orElse(null));
        contact.setLastFollowUpAt(last.map(FollowUp::getScheduledAt).orElse(null));
        contactRepository.save(contact);

        log.info("Contact follow-up dates refreshed | contactId={} next={} last={}",
                contact.getId(),
                contact.getNextFollowUpAt(),
                contact.getLastFollowUpAt());
    }
}
