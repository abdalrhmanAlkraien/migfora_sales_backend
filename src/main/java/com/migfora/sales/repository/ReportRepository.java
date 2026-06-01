package com.migfora.sales.repository;

import com.migfora.sales.entity.Report;
import com.migfora.sales.entity.Report.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:33 PM
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByCompanyId(Long companyId, Pageable pageable);

    List<Report> findByInvestigationId(Long investigationId);

    List<Report> findByCompanyIdAndType(Long companyId, ReportType type);
    long countByCompanyId(Long companyId);

}
