package com.migfora.sales.repository;

import com.migfora.sales.entity.InvestigationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:36 PM
 */
@Repository
public interface InvestigationContextRepository extends JpaRepository<InvestigationContext, Long> {
    Optional<InvestigationContext> findByInvestigationId(Long investigationId);
}
