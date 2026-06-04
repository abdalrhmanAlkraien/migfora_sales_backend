package com.migfora.sales.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:57 PM
 */

@Component
@Order(2)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String MDC_USER_ID = "userId";

    private boolean isPollingRequest(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod())) return false;
        String path = request.getRequestURI();

        // Match exact polling patterns
        // e.g. GET /api/v1/reports/1  or  GET /api/v1/investigations/5/tasks/12
        return (path.matches("/api/v1/reports/\\d+") ||
                path.matches("/api/v1/investigations/\\d+/tasks/\\d+") ||
                path.matches("/api/v1/investigations/\\d+/tasks"));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {


        if (isPollingRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();

        log.info(">>> {} {} [corrId={}]",
                request.getMethod(),
                request.getRequestURI(),
                MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("<<< {} {} | status={} | {}ms [corrId={}]",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID));
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip noise from actuator health checks
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
