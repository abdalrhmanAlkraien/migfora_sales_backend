package com.migfora.sales.repository;

import com.migfora.sales.entity.MessageTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:47 PM
 */
public interface MessageTemplateRepository
        extends JpaRepository<MessageTemplate, Long> {

    @Query(value = """
    SELECT * FROM message_templates m
    WHERE (CAST(:type AS text) IS NULL OR m.type = CAST(:type AS text))
    AND (CAST(:doorType AS text) IS NULL OR m.door_type = CAST(:doorType AS text))
    AND (CAST(:channel AS text) IS NULL OR m.channel = CAST(:channel AS text))
    AND (CAST(:language AS text) IS NULL OR m.language = CAST(:language AS text))
    AND (:activeOnly = false OR m.active = true)
    AND (CAST(:search AS text) IS NULL OR
         LOWER(m.title) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
         LOWER(COALESCE(m.tags, '')) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
    ORDER BY m.updated_at DESC
    """, countQuery = """
    SELECT COUNT(*) FROM message_templates m
    WHERE (CAST(:type AS text) IS NULL OR m.type = CAST(:type AS text))
    AND (CAST(:doorType AS text) IS NULL OR m.door_type = CAST(:doorType AS text))
    AND (CAST(:channel AS text) IS NULL OR m.channel = CAST(:channel AS text))
    AND (CAST(:language AS text) IS NULL OR m.language = CAST(:language AS text))
    AND (:activeOnly = false OR m.active = true)
    AND (CAST(:search AS text) IS NULL OR
         LOWER(m.title) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')) OR
         LOWER(COALESCE(m.tags, '')) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
    """, nativeQuery = true)
    Page<MessageTemplate> search(
            @Param("type")       String type,
            @Param("doorType")   String doorType,
            @Param("channel")    String channel,
            @Param("language")   String language,
            @Param("activeOnly") boolean activeOnly,
            @Param("search")     String search,
            Pageable pageable
    );
}