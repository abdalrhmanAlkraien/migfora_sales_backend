package com.migfora.sales.repository;

import com.migfora.sales.entity.CompanyPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 04/06/2026
 * @Time: 12:02 PM
 */
public interface CompanyPlatformRepository extends JpaRepository<CompanyPlatform, Long> {

    List<CompanyPlatform> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    long countByCompanyId(Long companyId);
}
