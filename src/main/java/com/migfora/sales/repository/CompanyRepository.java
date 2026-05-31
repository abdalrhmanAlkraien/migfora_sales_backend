package com.migfora.sales.repository;

import com.migfora.sales.enitty.Company;
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

    Optional<Company> findByDomain(String domain);

    boolean existsByDomain(String domain);

    Page<Company> findByCreatedBy(String createdBy, Pageable pageable);

    Page<Company> findByStatus(Company.CompanyStatus status, Pageable pageable);

    @Query("""
            SELECT c FROM Company c
            WHERE (:search IS NULL OR
                   LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(c.domain) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:status IS NULL OR c.status = :status)
            """)
    Page<Company> search(@Param("search") String search,
                         @Param("status") Company.CompanyStatus status,
                         Pageable pageable);
}
