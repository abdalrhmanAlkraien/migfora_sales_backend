package com.migfora.sales.repository;

import com.migfora.sales.entity.CompanyPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 04/06/2026
 * @Time: 12:02 PM
 */
public interface CompanyPlatformRepository extends JpaRepository<CompanyPlatform, Long> {

    List<CompanyPlatform> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    long countByCompanyId(Long companyId);
    List<CompanyPlatform> findByCompanyIdIn(List<Long> companyIds);

    @Query("""
    SELECT p FROM CompanyPlatform p
    JOIN FETCH p.company
    WHERE p.company.id IN :companyIds
    ORDER BY p.createdAt DESC
    """)
    List<CompanyPlatform> findByCompanyIdInWithCompany(
            @Param("companyIds") List<Long> companyIds);

}
