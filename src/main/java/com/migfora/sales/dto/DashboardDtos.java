package com.migfora.sales.dto;

import com.migfora.sales.entity.Contact.ContactStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 12:54 PM
 */
public class DashboardDtos {


    public record DashboardStatsResponse(
            long totalCompanies,
            long totalContacts,
            long totalInvestigations,
            long totalReports,
            long totalUsers,
            long pendingFollowUps,
            long followUpsDueToday
    ) {}

    public record TodayFollowUpResponse(
            Long id,
            Long contactId,
            String contactName,
            Long companyId,
            String companyName,
            String type,
            String status,
            LocalDateTime scheduledAt,
            String notes
    ) {}

    public record InvestigationSummaryResponse(
            Long id,
            String domain,
            Long companyId,
            String companyName,
            String status,
            String ipAddress,
            LocalDateTime createdAt
    ) {}

    public record ContactStatsResponse(
            Map<ContactStatus, Long> byStatus
    ) {}
}
