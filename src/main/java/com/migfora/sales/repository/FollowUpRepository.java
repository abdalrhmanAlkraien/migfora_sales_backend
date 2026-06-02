package com.migfora.sales.repository;

import com.migfora.sales.entity.FollowUp;
import com.migfora.sales.entity.FollowUp.FollowUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 02/06/2026
 * @Time: 12:17 AM
 */
@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    @Query(value = """
        SELECT * FROM follow_ups f
        WHERE f.contact_id = :contactId
        ORDER BY f.scheduled_at ASC
        """,
            countQuery = "SELECT COUNT(*) FROM follow_ups WHERE contact_id = :contactId",
            nativeQuery = true)
    Page<FollowUp> findByContactId(@Param("contactId") Long contactId, Pageable pageable);

    long countByContactId(Long contactId);

    long countByContactIdAndStatus(Long contactId, FollowUpStatus status);

    Optional<FollowUp> findFirstByContactIdAndStatusOrderByScheduledAtAsc(
            Long contactId, FollowUpStatus status);

    Optional<FollowUp> findFirstByContactIdAndStatusOrderByScheduledAtDesc(
            Long contactId, FollowUpStatus status);

    @Query("""
            SELECT f FROM FollowUp f
            JOIN FETCH f.contact c
            JOIN FETCH c.company
            WHERE f.status = 'SCHEDULED'
            AND f.scheduledAt >= :startOfDay
            AND f.scheduledAt <= :endOfDay
            ORDER BY f.scheduledAt ASC
            """)
    List<FollowUp> findTodayFollowUps(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
