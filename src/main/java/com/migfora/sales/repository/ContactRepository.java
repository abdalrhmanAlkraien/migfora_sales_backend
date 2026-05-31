package com.migfora.sales.repository;


import com.migfora.sales.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:23 PM
 */
@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    Page<Contact> findByCompanyId(Long companyId, Pageable pageable);

    @Query("""
            SELECT c FROM Contact c
            WHERE c.company.id = :companyId
            AND (:search IS NULL OR
                 LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Contact> searchByCompany(@Param("companyId") Long companyId,
                                  @Param("search") String search,
                                  Pageable pageable);
}
