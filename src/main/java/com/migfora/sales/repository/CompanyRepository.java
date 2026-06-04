package com.migfora.sales.repository;

import com.migfora.sales.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:14 PM
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByWebsite(String website);

    @Query(value = """
        SELECT * FROM companies c
        WHERE (CAST(:search AS text) IS NULL OR
               LOWER(CAST(c.name AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
               OR LOWER(CAST(c.domain AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
        AND (CAST(:status AS text) IS NULL OR CAST(c.status AS text) = CAST(:status AS text))
        ORDER BY c.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM companies c
        WHERE (CAST(:search AS text) IS NULL OR
               LOWER(CAST(c.name AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
               OR LOWER(CAST(c.domain AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
        AND (CAST(:status AS text) IS NULL OR CAST(c.status AS text) = CAST(:status AS text))
        """,
            nativeQuery = true)
    Page<Company> search(@Param("search") String search,
                         @Param("status") String status,
                         Pageable pageable);
}
