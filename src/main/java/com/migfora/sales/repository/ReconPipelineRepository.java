package com.migfora.sales.repository;

import com.migfora.sales.entity.ReconPipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:27 PM
 */
@Repository
public interface ReconPipelineRepository extends JpaRepository<ReconPipeline, Long> {

    List<ReconPipeline> findByCreatedBy(String createdBy);

    Optional<ReconPipeline> findByIsDefaultTrue();

    boolean existsByName(String name);
}
