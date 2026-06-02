package com.migfora.sales.job;

import com.migfora.sales.entity.FollowUp;
import com.migfora.sales.repository.FollowUpRepository;
import com.migfora.sales.service.CognitoAdminService;
import com.migfora.sales.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 02/06/2026
 * @Time: 4:27 AM
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.reminder.enabled", havingValue = "true")
public class FollowUpReminderJob {


    private final FollowUpRepository followUpRepository;
    private final CognitoAdminService cognitoAdminService;
    private final EmailService emailService;

    @Value("${app.reminder.cron:0 0 8 * * *}")
    private String cron;

    @Scheduled(cron = "${app.reminder.cron:0 0 8 * * *}",
            zone = "${app.reminder.timezone:Asia/Riyadh}")
    public void sendDailyReminders() {
        log.info("FollowUpReminderJob started | date={}", LocalDate.now());

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().atTime(23, 59, 59);

        List<FollowUp> todayFollowUps = followUpRepository
                .findTodayFollowUps(startOfDay, endOfDay);

        if (todayFollowUps.isEmpty()) {
            log.info("No follow-ups scheduled for today — skipping reminders");
            return;
        }

        // Group by createdBy (sales user sub)
        Map<String, List<FollowUp>> byUser = todayFollowUps.stream()
                .collect(Collectors.groupingBy(FollowUp::getCreatedBy));

        log.info("Sending reminders | totalFollowUps={} users={}",
                todayFollowUps.size(), byUser.size());

        byUser.forEach((userSub, followUps) -> {
            try {
                sendReminderToUser(userSub, followUps);
            } catch (Exception ex) {
                log.error("Failed to send reminder | userSub={} error={}",
                        userSub, ex.getMessage());
            }
        });

        log.info("FollowUpReminderJob completed");
    }

    private void sendReminderToUser(String userSub, List<FollowUp> followUps) {
        // Get user email and name from Cognito
        var userInfo = cognitoAdminService.getUserBySub(userSub);
        String email    = userInfo.email();
        String userName = userInfo.name() != null
                ? userInfo.name() : email;

        if (email == null || email.isBlank()) {
            log.warn("No email found for user | sub={}", userSub);
            return;
        }

        // Build template data
        List<Map<String, String>> followUpData = followUps.stream()
                .map(f -> {
                    Map<String, String> data = new LinkedHashMap<>();
                    data.put("contactName", f.getContact().getName());
                    data.put("companyName", f.getContact().getCompany().getName());
                    data.put("type",        f.getType().name());
                    data.put("time",        f.getScheduledAt().format(
                            DateTimeFormatter.ofPattern("hh:mm a")));
                    data.put("notes",       f.getNotes());
                    return data;
                })
                .collect(Collectors.toList());

        String today = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d", new Locale("ar")));

        Context context = new Context();
        context.setVariable("userName",  userName);
        context.setVariable("followUps", followUpData);
        context.setVariable("today",     today);

        String html = emailService.renderTemplate(
                "emails/follow-up-reminder", context);

        String subject = "📅 متابعاتك اليوم — " + followUps.size() + " مواعيد";

        emailService.sendHtml(email, subject, html);

        log.info("Reminder sent | to={} followUps={}", email, followUps.size());
    }
}
