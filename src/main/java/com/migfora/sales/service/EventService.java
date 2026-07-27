package com.migfora.sales.service;

import com.migfora.sales.dto.EventDtos.*;
import com.migfora.sales.entity.Event;
import com.migfora.sales.exception.ResourceNotFoundException;
import com.migfora.sales.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:45 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;

    @Transactional
    public EventResponse create(CreateEventRequest request, String createdBy) {
        Event event = Event.builder()
                .name(request.name())
                .description(request.description())
                .website(request.website())
                .linkedinUrl(request.linkedinUrl())
                .country(request.country())
                .city(request.city())
                .venue(request.venue())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .registrationDeadline(request.registrationDeadline())
                .attendanceType(request.attendanceType())
                .industry(request.industry())
                .expectedAttendees(request.expectedAttendees())
                .cost(request.cost())
                .notes(request.notes())
                .status(request.status() != null
                        ? request.status() : Event.EventStatus.UPCOMING)
                .createdBy(createdBy)
                .build();

        event = eventRepository.save(event);
        log.info("[Event] Created | id={} name={} by={}",
                event.getId(), event.getName(), createdBy);
        return toResponse(event);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getAll(String country,
                                      String city,
                                      Event.EventStatus status,
                                      Integer month,
                                      Integer year,
                                      LocalDate startFrom,
                                      LocalDate startTo,
                                      Pageable pageable) {
        // Strip sort — native query handles ORDER BY e.start_date ASC
        Pageable unsorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return eventRepository.search(
                country,
                city,
                status != null ? status.name() : null,
                month,
                year,
                startFrom,
                startTo,
                unsorted    // ← pass unsorted
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public EventResponse update(Long id,
                                UpdateEventRequest request,
                                String updatedBy) {
        Event event = findById(id);

        if (request.name()                 != null) event.setName(request.name());
        if (request.description()          != null) event.setDescription(request.description());
        if (request.website()              != null) event.setWebsite(request.website());
        if (request.linkedinUrl()          != null) event.setLinkedinUrl(request.linkedinUrl());
        if (request.country()              != null) event.setCountry(request.country());
        if (request.city()                 != null) event.setCity(request.city());
        if (request.venue()                != null) event.setVenue(request.venue());
        if (request.startDate()            != null) event.setStartDate(request.startDate());
        if (request.endDate()              != null) event.setEndDate(request.endDate());
        if (request.registrationDeadline() != null) event.setRegistrationDeadline(request.registrationDeadline());
        if (request.attendanceType()       != null) event.setAttendanceType(request.attendanceType());
        if (request.industry()             != null) event.setIndustry(request.industry());
        if (request.expectedAttendees()    != null) event.setExpectedAttendees(request.expectedAttendees());
        if (request.cost()                 != null) event.setCost(request.cost());
        if (request.notes()                != null) event.setNotes(request.notes());
        if (request.status()               != null) event.setStatus(request.status());

        event = eventRepository.save(event);
        log.info("[Event] Updated | id={} by={}", id, updatedBy);
        return toResponse(event);
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        eventRepository.deleteById(id);
        log.info("[Event] Deleted | id={} by={}", id, deletedBy);
    }

    private Event findById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found: " + id));
    }

    private EventResponse toResponse(Event e) {
        return new EventResponse(
                e.getId(), e.getName(), e.getDescription(),
                e.getWebsite(), e.getLinkedinUrl(),
                e.getCountry(), e.getCity(), e.getVenue(),
                e.getStartDate(), e.getEndDate(),
                e.getRegistrationDeadline(),
                e.getAttendanceType(), e.getIndustry(),
                e.getExpectedAttendees(), e.getCost(),
                e.getNotes(), e.getStatus(), e.getCreatedBy(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
