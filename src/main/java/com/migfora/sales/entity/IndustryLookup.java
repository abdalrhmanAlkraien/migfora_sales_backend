package com.migfora.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/07/2026
 * @Time: 6:57 AM
 */
@Entity
@Table(name = "industry_lookups")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IndustryLookup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;              // e.g. "E-Commerce", "FinTech", "Healthcare"

    private String nameAr;            // Arabic name for future Arabic support

    private String description;

    @Builder.Default
    private boolean active = true;    // soft disable without deleting

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
