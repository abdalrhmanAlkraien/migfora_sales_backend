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

    @Query(value = """
        SELECT * FROM contacts c
        WHERE c.company_id = :companyId
        AND (CAST(:search AS text) IS NULL OR
             LOWER(CAST(c.name AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
             OR LOWER(CAST(c.email AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
        ORDER BY c.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM contacts c
        WHERE c.company_id = :companyId
        AND (CAST(:search AS text) IS NULL OR
             LOWER(CAST(c.name AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%'))
             OR LOWER(CAST(c.email AS text)) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
        """,
            nativeQuery = true)
    Page<Contact> searchByCompany(@Param("companyId") Long companyId,
                                  @Param("search") String search,
                                  Pageable pageable);

    long countByCompanyId(Long companyId);

}
