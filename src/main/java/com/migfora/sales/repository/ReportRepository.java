package com.migfora.sales.repository;

import com.migfora.sales.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:33 PM
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r WHERE r.platform.company.id = :companyId")
    Page<Report> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    List<Report> findByStatus(Report.ReportStatus status);

    long countByPlatformId(Long platformId);


    Page<Report> findByPlatformId(Long platformId, Pageable pageable);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.platform.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(r) FROM Report r WHERE r.platform.id IN :platformIds")
    long countByPlatformIdIn(@Param("platformIds") List<Long> platformIds);

    @Query("""
    SELECT r FROM Report r
    JOIN FETCH r.platform p
    JOIN FETCH p.company
    LEFT JOIN FETCH r.investigation
    WHERE r.id = :id
    """)
    Optional<Report> findByIdWithPlatform(@Param("id") Long id);
}
