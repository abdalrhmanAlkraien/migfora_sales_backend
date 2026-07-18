package com.migfora.sales.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Log4j2
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/change-password",
            "/api/v1/auth/forgot-password",           // ← add
            "/api/v1/auth/confirm-forgot-password",   // ← add
            "/actuator/health",
            "/actuator/info",
            "/api-docs",
            "/api-docs/**",
            "/api-docs/swagger-config",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**", "/actuator/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(cognitoJwtConverter())
                        )
                        .authenticationEntryPoint((request, response, ex) -> {
                            // Log the reason
                            String path  = request.getRequestURI();
                            String token = request.getHeader("Authorization");
                            String reason = ex.getMessage() != null ? ex.getMessage() : "Unknown";

                            if (reason.toLowerCase().contains("expired")) {
                                log.warn("[Auth] Token expired | path={} reason={}", path, reason);
                            } else if (token == null || token.isBlank()) {
                                log.warn("[Auth] Missing token | path={}", path);
                            } else {
                                log.warn("[Auth] Invalid token | path={} reason={}", path, reason);
                            }

                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            new ObjectMapper().writeValue(response.getWriter(),
                                    Map.of(
                                            "error",   "Unauthorized",
                                            "message", "Missing or invalid token"
                                    ));
                        })
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("[Auth] Access denied | path={} reason={}",
                                    request.getRequestURI(),
                                    accessDeniedException.getMessage());

                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            new ObjectMapper().writeValue(response.getWriter(),
                                    Map.of(
                                            "error",   "Forbidden",
                                            "message", "You don't have permission"
                                    ));
                        })
                )
                .build();
    }
    @Bean
    public JwtAuthenticationConverter cognitoJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();

            Object groups = jwt.getClaims().get("cognito:groups");

            if (groups instanceof List<?> groupList) {
                groupList.stream()
                        .map(g -> new SimpleGrantedAuthority("ROLE_" + g.toString().toUpperCase()))
                        .forEach(authorities::add);
            }

            return authorities;
        });

        // Use 'sub' claim as principal name (Cognito user UUID)
        converter.setPrincipalClaimName("sub");

        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}