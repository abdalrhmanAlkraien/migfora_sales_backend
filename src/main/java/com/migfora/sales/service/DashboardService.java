package com.migfora.sales.service;

import com.migfora.sales.dto.DashboardDtos.*;
import com.migfora.sales.entity.Contact.ContactStatus;
import com.migfora.sales.entity.FollowUp;
import com.migfora.sales.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 12:56 PM
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CompanyRepository        companyRepository;
    private final ContactRepository        contactRepository;
    private final InvestigationRepository  investigationRepository;
    private final ReportRepository         reportRepository;
    private final FollowUpRepository       followUpRepository;
    private final UserManagementService    userManagementService;

    // ── Stats ─────────────────────────────────────────────────────────────────

    private LocalDateTime startOfDay() {
        return LocalDate.now().atStartOfDay();
    }

    private LocalDateTime endOfDay() {
        return LocalDate.now().atTime(23, 59, 59);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        long totalCompanies      = companyRepository.count();
        long totalContacts       = contactRepository.count();
        long totalInvestigations = investigationRepository.count();
        long totalReports        = reportRepository.count();
        long totalUsers          = userManagementService.countUsers();
        long pendingFollowUps    = followUpRepository
                .countByStatus(FollowUp.FollowUpStatus.SCHEDULED);
        long followUpsDueToday   = followUpRepository
                .countTodayScheduled(startOfDay(), endOfDay());

        return new DashboardStatsResponse(
                totalCompanies, totalContacts, totalInvestigations,
                totalReports, totalUsers, pendingFollowUps, followUpsDueToday
        );
    }

    @Transactional(readOnly = true)
    public Page<TodayFollowUpResponse> getTodayFollowUps(Pageable pageable) {
        return followUpRepository
                .findTodayScheduled(startOfDay(), endOfDay(), pageable)
                .map(this::toTodayFollowUpResponse);
    }

    // ── Recent investigations ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<InvestigationSummaryResponse> getRecentInvestigations(Pageable pageable) {
        return investigationRepository.findAll(pageable)
                .map(this::toInvestigationSummary);
    }

    // ── Contact stats ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ContactStatsResponse getContactStats() {
        List<Object[]> rows = contactRepository.countByStatus();
        Map<ContactStatus, Long> byStatus = new LinkedHashMap<>();

        // Initialize all statuses with 0
        for (ContactStatus status : ContactStatus.values()) {
            byStatus.put(status, 0L);
        }

        // Fill from query
        for (Object[] row : rows) {
            ContactStatus status = (ContactStatus) row[0];
            Long count           = (Long) row[1];
            byStatus.put(status, count);
        }

        return new ContactStatsResponse(byStatus);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private TodayFollowUpResponse toTodayFollowUpResponse(FollowUp f) {
        return new TodayFollowUpResponse(
                f.getId(),
                f.getContact().getId(),
                f.getContact().getName(),
                f.getContact().getCompany().getId(),
                f.getContact().getCompany().getName(),
                f.getType().name(),
                f.getStatus().name(),
                f.getScheduledAt(),
                f.getNotes()
        );
    }

    private InvestigationSummaryResponse toInvestigationSummary(
            com.migfora.sales.entity.Investigation i) {
        return new InvestigationSummaryResponse(
                i.getId(),
                i.getDomain(),
                i.getCompany().getId(),
                i.getCompany().getName(),
                i.getStatus().name(),
                i.getIpAddress(),
                i.getCreatedAt()
        );
    }
}
