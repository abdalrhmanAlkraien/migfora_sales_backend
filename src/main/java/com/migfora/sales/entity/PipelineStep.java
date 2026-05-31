package com.migfora.sales.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 4:25 PM
 */

@Entity
@Table(name = "pipeline_steps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PipelineStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int executionOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReconTask.ReconTaskType taskType;

    @Column(nullable = false)
    @Builder.Default
    private boolean stopOnFailure = true;

    // If CDN detected on this step — should we continue?
    @Builder.Default
    private boolean continueOnCdn = true;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id")
    private ReconPipeline pipeline;
}
