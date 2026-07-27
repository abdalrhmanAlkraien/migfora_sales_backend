package com.migfora.sales.controller;

import com.migfora.sales.dto.EventDtos.*;
import com.migfora.sales.entity.Event;
import com.migfora.sales.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:46 PM
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Upcoming industry events")
@PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "Create event")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return eventService.create(request, jwt.getSubject());
    }

    @Operation(summary = "List events with filters")
    @GetMapping
    public Page<EventResponse> getAll(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Event.EventStatus status,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return eventService.getAll(
                country, city, status, month, year, startFrom, startTo, pageable);
    }

    @Operation(summary = "Get event by ID")
    @GetMapping("/{id}")
    public EventResponse getById(@PathVariable Long id) {
        return eventService.getById(id);
    }

    @Operation(summary = "Update event")
    @PatchMapping("/{id}")
    public EventResponse update(
            @PathVariable Long id,
            @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return eventService.update(id, request, jwt.getSubject());
    }

    @Operation(summary = "Delete event")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN_GROUP')")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        eventService.delete(id, jwt.getSubject());
    }
}
