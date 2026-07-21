package com.migfora.sales.repository;

import com.migfora.sales.entity.IndustryLookup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/07/2026
 * @Time: 6:58 AM
 */
@Repository
public interface IndustryLookupRepository extends JpaRepository<IndustryLookup, Long> {

    List<IndustryLookup> findByActiveTrueOrderByNameAsc();

    Optional<IndustryLookup> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    Page<IndustryLookup> findByActiveTrue(Pageable pageable);

}