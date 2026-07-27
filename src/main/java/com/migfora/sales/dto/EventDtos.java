package com.migfora.sales.dto;

import com.migfora.sales.entity.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:45 PM
 */
public class EventDtos {

    public record CreateEventRequest(
            @NotBlank String name,
            String description,
            String website,
            String linkedinUrl,
            String country,
            String city,
            String venue,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            LocalDate registrationDeadline,
            Event.AttendanceType attendanceType,
            String industry,
            Integer expectedAttendees,
            String cost,
            String notes,
            Event.EventStatus status
    ) {}

    public record UpdateEventRequest(
            String name,
            String description,
            String website,
            String linkedinUrl,
            String country,
            String city,
            String venue,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationDeadline,
            Event.AttendanceType attendanceType,
            String industry,
            Integer expectedAttendees,
            String cost,
            String notes,
            Event.EventStatus status
    ) {}

    public record EventResponse(
            Long id,
            String name,
            String description,
            String website,
            String linkedinUrl,
            String country,
            String city,
            String venue,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate registrationDeadline,
            Event.AttendanceType attendanceType,
            String industry,
            Integer expectedAttendees,
            String cost,
            String notes,
            Event.EventStatus status,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}
