package com.migfora.sales.controller;

import com.migfora.sales.dto.DashboardDtos.*;
import com.migfora.sales.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 12:57 PM
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard stats and summary endpoints")
public class DashboardController {


    private final DashboardService dashboardService;

    // ── Admin + Sales ─────────────────────────────────────────────────────────

    @Operation(summary = "Get dashboard stats summary")
    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @Operation(summary = "Get follow-ups due today")
    @GetMapping("/followups/today")
    @PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
    public ResponseEntity<Page<TodayFollowUpResponse>> getTodayFollowUps(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getTodayFollowUps(pageable));
    }

    // ── Admin only ────────────────────────────────────────────────────────────

    @Operation(summary = "Get recent investigations")
    @GetMapping("/investigations")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    public ResponseEntity<Page<InvestigationSummaryResponse>> getRecentInvestigations(
            @PageableDefault(size = 5, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(
                dashboardService.getRecentInvestigations(pageable));
    }

    @Operation(summary = "Get contact pipeline stats")
    @GetMapping("/contacts/stats")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    public ResponseEntity<ContactStatsResponse> getContactStats() {
        return ResponseEntity.ok(dashboardService.getContactStats());
    }
}
