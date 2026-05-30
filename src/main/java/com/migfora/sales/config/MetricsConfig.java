package com.migfora.sales.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:58 PM
 */
@Configuration
public class MetricsConfig {

    // ── Counters ──────────────────────────────────────────────────────────────

    @Bean
    public Counter registerSuccessCounter(MeterRegistry registry) {
        return Counter.builder("auth.register.success")
                .description("Total successful user registrations")
                .register(registry);
    }

    @Bean
    public Counter registerFailureCounter(MeterRegistry registry) {
        return Counter.builder("auth.register.failure")
                .description("Total failed user registrations")
                .register(registry);
    }

    @Bean
    public Counter loginSuccessCounter(MeterRegistry registry) {
        return Counter.builder("auth.login.success")
                .description("Total successful logins")
                .register(registry);
    }

    @Bean
    public Counter loginFailureCounter(MeterRegistry registry) {
        return Counter.builder("auth.login.failure")
                .description("Total failed logins (bad credentials, disabled)")
                .register(registry);
    }

    @Bean
    public Counter tokenRefreshCounter(MeterRegistry registry) {
        return Counter.builder("auth.token.refresh")
                .description("Total token refresh operations")
                .register(registry);
    }

    @Bean
    public Counter logoutCounter(MeterRegistry registry) {
        return Counter.builder("auth.logout")
                .description("Total logout operations")
                .register(registry);
    }

    // ── Timers ────────────────────────────────────────────────────────────────

    @Bean
    public Timer loginTimer(MeterRegistry registry) {
        return Timer.builder("auth.login.duration")
                .description("Login request processing time")
                .register(registry);
    }

    @Bean
    public Timer registerTimer(MeterRegistry registry) {
        return Timer.builder("auth.register.duration")
                .description("Register request processing time")
                .register(registry);
    }
}
