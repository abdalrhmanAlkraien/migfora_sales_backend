package com.migfora.sales.service;

import com.migfora.sales.dto.MessageTemplateDtos.*;
import com.migfora.sales.entity.MessageTemplate;
import com.migfora.sales.exception.ResourceNotFoundException;
import com.migfora.sales.repository.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/07/2026
 * @Time: 6:47 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageTemplateService {

    private final MessageTemplateRepository templateRepository;

    @Transactional
    public TemplateResponse create(CreateTemplateRequest request,
                                   String createdBy) {
        MessageTemplate template = MessageTemplate.builder()
                .title(request.title())
                .subject(request.subject())
                .content(request.content())
                .type(request.type())
                .doorType(request.doorType())
                .channel(request.channel())
                .language(request.language() != null
                        ? request.language()
                        : MessageTemplate.TemplateLanguage.EN)
                .tags(request.tags())
                .active(true)
                .createdBy(createdBy)
                .build();

        template = templateRepository.save(template);
        log.info("[Template] Created | id={} title={} by={}",
                template.getId(), template.getTitle(), createdBy);
        return toResponse(template);
    }

    @Transactional(readOnly = true)
    public Page<TemplateListResponse> getAll(
            MessageTemplate.TemplateType type,
            MessageTemplate.DoorType doorType,
            MessageTemplate.TemplateChannel channel,
            MessageTemplate.TemplateLanguage language,
            boolean activeOnly,
            String search,
            Pageable pageable) {
        return templateRepository.search(
                type     != null ? type.name()     : null,   // ← String
                doorType != null ? doorType.name() : null,   // ← String
                channel  != null ? channel.name()  : null,   // ← String
                language != null ? language.name() : null,   // ← String
                activeOnly,
                search,
                pageable
        ).map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public TemplateResponse update(Long id,
                                   UpdateTemplateRequest request,
                                   String updatedBy) {
        MessageTemplate template = findById(id);

        if (request.title()    != null) template.setTitle(request.title());
        if (request.subject()  != null) template.setSubject(request.subject());
        if (request.content()  != null) template.setContent(request.content());
        if (request.type()     != null) template.setType(request.type());
        if (request.doorType() != null) template.setDoorType(request.doorType());
        if (request.channel()  != null) template.setChannel(request.channel());
        if (request.language() != null) template.setLanguage(request.language());
        if (request.tags()     != null) template.setTags(request.tags());
        if (request.active()   != null) template.setActive(request.active());

        template = templateRepository.save(template);
        log.info("[Template] Updated | id={} by={}", id, updatedBy);
        return toResponse(template);
    }

    @Transactional
    public void markAsUsed(Long id) {
        MessageTemplate template = findById(id);
        template.setLastUsedAt(LocalDateTime.now());
        templateRepository.save(template);
    }

    @Transactional
    public void delete(Long id, String deletedBy) {
        findById(id);
        templateRepository.deleteById(id);
        log.info("[Template] Deleted | id={} by={}", id, deletedBy);
    }

    private MessageTemplate findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Template not found: " + id));
    }

    private TemplateResponse toResponse(MessageTemplate t) {
        return new TemplateResponse(
                t.getId(), t.getTitle(), t.getSubject(), t.getContent(),
                t.getType(), t.getDoorType(), t.getChannel(), t.getLanguage(),
                t.getTags(), t.isActive(), t.getLastUsedAt(),
                t.getCreatedBy(), t.getCreatedAt(), t.getUpdatedAt()
        );
    }

    private TemplateListResponse toListResponse(MessageTemplate t) {
        return new TemplateListResponse(
                t.getId(), t.getTitle(), t.getSubject(),
                t.getType(), t.getDoorType(), t.getChannel(), t.getLanguage(),
                t.getTags(), t.isActive(), t.getLastUsedAt(), t.getCreatedAt()
        );
    }
}