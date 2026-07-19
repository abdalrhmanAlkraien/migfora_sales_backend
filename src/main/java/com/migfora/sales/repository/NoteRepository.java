package com.migfora.sales.repository;

import com.migfora.sales.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/07/2026
 * @Time: 12:56 PM
 */
public interface NoteRepository extends JpaRepository<Note, Long> {
    Page<Note> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);
    Page<Note> findByContactIdOrderByCreatedAtDesc(Long contactId, Pageable pageable);

}