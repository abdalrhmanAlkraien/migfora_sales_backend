package com.migfora.sales.service;

import com.migfora.sales.dto.NoteDtos.CreateNoteRequest;
import com.migfora.sales.dto.NoteDtos.NoteResponse;
import com.migfora.sales.dto.NoteDtos.UpdateNoteRequest;
import com.migfora.sales.entity.Company;
import com.migfora.sales.entity.Note;
import com.migfora.sales.exception.ResourceNotFoundException;
import com.migfora.sales.repository.CompanyRepository;
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

    // ── Add note ──────────────────────────────────────────────────────────────

    @Transactional
    public NoteResponse create(Long companyId,
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

        return toResponse(note);
    }

    @Transactional
    public List<NoteResponse> createBulkApi(Long companyId,
                                            List<CreateNoteRequest> requests,
                                            String createdBy) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Company not found: " + companyId));

        return requests.stream()
                .filter(r -> r.content() != null && !r.content().isBlank())
                .map(r -> {
                    Note note = Note.builder()
                            .company(company)
                            .content(r.content())
                            .createdBy(createdBy)
                            .build();
                    return toResponse(noteRepository.save(note));
                })
                .toList();
    }

    // ── Create multiple notes (used during company creation) ──────────────────

    @Transactional
    public void createBulk(Company company,
                           java.util.List<String> contents,
                           String createdBy) {
        if (contents == null || contents.isEmpty()) return;

        contents.stream()
                .filter(c -> c != null && !c.isBlank())
                .forEach(content -> {
                    Note note = Note.builder()
                            .company(company)
                            .content(content)
                            .createdBy(createdBy)
                            .build();
                    noteRepository.save(note);
                });

        log.info("[Note] Bulk created | company={} count={} by={}",
                company.getId(), contents.size(), createdBy);
    }

    // ── Get notes by company ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<NoteResponse> getByCompany(Long companyId, Pageable pageable) {
        return noteRepository
                .findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(this::toResponse);
    }

    // ── Update note ───────────────────────────────────────────────────────────

    @Transactional
    public NoteResponse update(Long id,
                               UpdateNoteRequest request,
                               String updatedBy) {
        Note note = findById(id);
        note.setContent(request.content());
        note = noteRepository.save(note);
        log.info("[Note] Updated | id={} by={}", id, updatedBy);
        return toResponse(note);
    }

    // ── Delete note ───────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        noteRepository.deleteById(id);
        log.info("[Note] Deleted | id={} by={}", id, deletedBy);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Note findById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found: " + id));
    }

    private NoteResponse toResponse(Note n) {
        return new NoteResponse(
                n.getId(),
                n.getCompany().getId(),
                n.getCompany().getName(),
                n.getContent(),
                n.getCreatedBy(),
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}