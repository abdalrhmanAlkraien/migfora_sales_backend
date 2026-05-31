package com.migfora.sales.repository;

import com.migfora.sales.entity.ReconTask;
import com.migfora.sales.entity.ReconTask.ReconTaskStatus;
import com.migfora.sales.entity.ReconTask.ReconTaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:54 PM
 */
@Repository
public interface ReconTaskRepository extends JpaRepository<ReconTask, Long> {

    List<ReconTask> findByInvestigationId(Long investigationId);

    List<ReconTask> findByInvestigationIdAndStatus(Long investigationId,
                                                   ReconTaskStatus status);

    Optional<ReconTask> findByInvestigationIdAndType(Long investigationId,
                                                     ReconTaskType type);

    boolean existsByInvestigationIdAndType(Long investigationId,
                                           ReconTaskType type);
}
