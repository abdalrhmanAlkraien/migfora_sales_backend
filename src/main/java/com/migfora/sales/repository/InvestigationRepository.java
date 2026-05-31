package com.migfora.sales.repository;

import com.migfora.sales.entity.Investigation;
import com.migfora.sales.entity.Investigation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:29 PM
 */
@Repository
public interface InvestigationRepository extends JpaRepository<Investigation, Long> {

    Page<Investigation> findByCompanyId(Long companyId, Pageable pageable);

    List<Investigation> findByCompanyIdAndStatus(Long companyId,
                                                 InvestigationStatus status);

    long countByCompanyId(Long companyId);
}
