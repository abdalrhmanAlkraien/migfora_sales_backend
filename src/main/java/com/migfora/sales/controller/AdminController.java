package com.migfora.sales.controller;

import com.migfora.sales.job.FollowUpReminderJob;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 02/06/2026
 * @Time: 4:29 AM
 */
@RestController
@RequestMapping("/api/v1/admin/jobs")
@Tag(name = "Admin", description = "Admin Controller")
@PreAuthorize("hasAnyRole('ADMIN_GROUP')")
@RequiredArgsConstructor
public class AdminController {

    private final FollowUpReminderJob followUpReminderJob;

    @Operation(summary = "Manually trigger follow-up reminder job — for testing")
    @PostMapping("/reminders/trigger")
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    public ResponseEntity<String> triggerReminders() {
        followUpReminderJob.sendDailyReminders();
        return ResponseEntity.ok("Reminder job triggered successfully");
    }
}
