package com.migfora.sales.repository;

import com.migfora.sales.entity.Investigation;
import com.migfora.sales.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:29 PM
 */
@Repository
public interface InvestigationRepository extends JpaRepository<Investigation, Long> {

    Page<Investigation> findAll(Pageable pageable);

    Page<Investigation> findByPlatformId(Long platformId, Pageable pageable);
    long countByPlatformId(Long platformId);

    @Query("SELECT COUNT(i) FROM Investigation i WHERE i.platform.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(i) FROM Investigation i WHERE i.platform.id IN :platformIds")
    long countByPlatformIdIn(@Param("platformIds") List<Long> platformIds);

    @Query("SELECT i FROM Investigation i WHERE i.platform.company.id = :companyId")
    Page<Investigation> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

}
