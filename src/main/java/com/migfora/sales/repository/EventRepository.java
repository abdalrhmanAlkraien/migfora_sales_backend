package com.migfora.sales.repository;

import com.migfora.sales.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:45 PM
 */
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query(value = """
        SELECT * FROM events e
        WHERE (:country IS NULL OR LOWER(e.country) = LOWER(CAST(:country AS text)))
        AND (:city IS NULL OR LOWER(e.city) = LOWER(CAST(:city AS text)))
        AND (CAST(:status AS text) IS NULL OR e.status = CAST(:status AS text))
        AND (:month IS NULL OR EXTRACT(MONTH FROM e.start_date) = :month)
        AND (:year IS NULL OR EXTRACT(YEAR FROM e.start_date) = :year)
        AND (CAST(:startFrom AS date) IS NULL OR e.start_date >= CAST(:startFrom AS date))
        AND (CAST(:startTo AS date) IS NULL OR e.start_date <= CAST(:startTo AS date))
        ORDER BY e.start_date ASC
        """, countQuery = """
        SELECT COUNT(*) FROM events e
        WHERE (:country IS NULL OR LOWER(e.country) = LOWER(CAST(:country AS text)))
        AND (:city IS NULL OR LOWER(e.city) = LOWER(CAST(:city AS text)))
        AND (CAST(:status AS text) IS NULL OR e.status = CAST(:status AS text))
        AND (:month IS NULL OR EXTRACT(MONTH FROM e.start_date) = :month)
        AND (:year IS NULL OR EXTRACT(YEAR FROM e.start_date) = :year)
        AND (CAST(:startFrom AS date) IS NULL OR e.start_date >= CAST(:startFrom AS date))
        AND (CAST(:startTo AS date) IS NULL OR e.start_date <= CAST(:startTo AS date))
        """, nativeQuery = true)
    Page<Event> search(
            @Param("country")   String country,
            @Param("city")      String city,
            @Param("status")    String status,
            @Param("month")     Integer month,
            @Param("year")      Integer year,
            @Param("startFrom") java.time.LocalDate startFrom,
            @Param("startTo")   java.time.LocalDate startTo,
            Pageable pageable
    );
}
