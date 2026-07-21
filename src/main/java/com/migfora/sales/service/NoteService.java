package com.migfora.sales.service;

import com.migfora.sales.dto.NoteDtos.CreateNoteRequest;
import com.migfora.sales.dto.NoteDtos.NoteResponse;
import com.migfora.sales.dto.NoteDtos.UpdateNoteRequest;
import com.migfora.sales.dto.UserDtos;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Contact;
import com.migfora.sales.entity.Note;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.exception.ResourceNotFoundException;
import com.migfora.sales.repository.CompanyRepository;
import com.migfora.sales.repository.ContactRepository;
import com.migfora.sales.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/07/2026
 * @Time: 12:56 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class NoteService {

    private final NoteRepository noteRepository;
    private final CompanyRepository companyRepository;
    private final UserManagementService userManagementService;
    private final ContactRepository contactRepository;

    // ── Add note ──────────────────────────────────────────────────────────────

    @Transactional
    public NoteResponse createForCompany(Long companyId,
                               CreateNoteRequest request,
                               String createdBy) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found: " + companyId));

        Note note = Note.builder()
                .company(company)
                .content(request.content())
                .createdBy(createdBy)
                .build();

        note = noteRepository.save(note);
        log.info("[Note] Created | id={} company={} by={}",
                note.getId(), companyId, createdBy);

        return toResponse(note, createdBy);
    }

    @Transactional
    public List<NoteResponse> createBulkApi(Long companyId,
                                            List<CreateNoteRequest> requests,
                                            String createdBy) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found: " + companyId));
        String createdByName = resolveUserName(createdBy);
        return requests.stream()
                .filter(r -> r.content() != null && !r.content().isBlank())
                .map(r -> {
                    Note note = Note.builder()
                            .type(Note.NoteType.COMPANY)    // ← add this
                            .company(company)
                            .content(r.content())
                            .createdBy(createdBy)
                            .createdByName(createdByName)
                            .build();
                    return toResponse(noteRepository.save(note), createdBy);
                })
                .toList();
    }

    // ── Create multiple notes (used during company creation) ──────────────────

    @Transactional
    public void createBulk(Company company,
                           List<String> contents,
                           String createdBy) {
        if (contents == null || contents.isEmpty()) return;
        String createdByName = resolveUserName(createdBy);
        contents.stream()
                .filter(c -> c != null && !c.isBlank())
                .forEach(content -> noteRepository.save(
                        Note.builder()
                                .type(Note.NoteType.COMPANY)    // ← this must be here
                                .company(company)
                                .content(content)
                                .createdBy(createdBy)
                                .createdByName(createdByName)
                                .build()
                ));
        log.info("[Note] Bulk created for company | company={} count={} by={}",
                company.getId(), contents.size(), createdBy);
    }

    // ── Get notes by company ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NoteResponse> getByCompany(Long companyId, String createdBy, Pageable pageable) {
        return noteRepository
                .findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(note -> toResponse(noteRepository.save(note), createdBy));
    }

    @Transactional
    public List<NoteResponse> createBulkForContact(Long contactId,
                                                   List<CreateNoteRequest> requests,
                                                   String createdBy) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contact not found: " + contactId));
        String createdByName = resolveUserName(createdBy);
        return requests.stream()
                .filter(r -> r.content() != null && !r.content().isBlank())
                .map(r -> {
                    Note note = Note.builder()
                            .type(Note.NoteType.CONTACT)
                            .contact(contact)
                            .content(r.content())
                            .createdBy(createdBy)
                            .createdByName(createdByName)
                            .build();
                    return toResponse(noteRepository.save(note), createdBy);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<NoteResponse> getByContact(Long contactId, String createdBy, Pageable pageable) {
        return noteRepository
                .findByContactIdOrderByCreatedAtDesc(contactId, pageable)
                .map(note -> toResponse(noteRepository.save(note), createdBy));
    }

    // ── Shared update/delete with ownership check ─────────────────────────────

    @Transactional
    public NoteResponse update(Long id,
                               UpdateNoteRequest request,
                               String requestedBy) {
        Note note = findById(id);
        checkOwnership(note, requestedBy);
        note.setContent(request.content());
        note = noteRepository.save(note);
        log.info("[Note] Updated | id={} by={}", id, requestedBy);
        return toResponse(note, requestedBy);
    }

    @Transactional
    public void delete(Long id, String requestedBy) {
        Note note = findById(id);
        checkOwnership(note, requestedBy);
        noteRepository.deleteById(id);
        log.info("[Note] Deleted | id={} by={}", id, requestedBy);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void checkOwnership(Note note, String requestedBy) {
        if (!note.getCreatedBy().equals(requestedBy)) {
            throw new AuthException(
                    "You can only update or delete your own notes.");
        }
    }

    private String resolveUserName(String sub) {
        try {
            UserDtos.UserDetailResponse user =
                    userManagementService.getUserBySub(sub);
            return user.name() + " " + user.familyName();
        } catch (Exception ex) {
            log.warn("[Note] Could not resolve user name | sub={}", sub);
            return sub;
        }
    }

    private Note findById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found: " + id));
    }

    private NoteResponse toResponse(Note n, String currentUserSub) {
        return new NoteResponse(
                n.getId(),
                n.getType(),
                n.getCompany()  != null ? n.getCompany().getId()  : null,
                n.getContact()  != null ? n.getContact().getId()  : null,
                n.getContent(),
                n.getCreatedBy(),
                n.getCreatedByName(),
                n.getCreatedBy().equals(currentUserSub),   // ← isOwner
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }

}