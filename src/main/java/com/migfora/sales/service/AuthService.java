package com.migfora.sales.service;

import com.migfora.sales.dto.AuthDtos.*;
import com.migfora.sales.enitty.RefreshToken;
import com.migfora.sales.enitty.User;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.MDC;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:42 PM
 */

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;


    // Injected from MetricsConfig
    private final Counter registerSuccessCounter;
    private final Counter registerFailureCounter;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter tokenRefreshCounter;
    private final Counter logoutCounter;
    private final Timer loginTimer;
    private final Timer   registerTimer;

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return registerTimer.record(() -> {
            log.info("Register attempt | email={} username={} corrId={}",
                    request.email(), request.username(),
                    MDC.get("correlationId"));

            if (userRepository.existsByEmail(request.email())) {
                registerFailureCounter.increment();
                log.warn("Register failed — email already exists | email={}", request.email());
                throw new AuthException("Email is already registered.");
            }
            if (userRepository.existsByUsername(request.username())) {
                registerFailureCounter.increment();
                log.warn("Register failed — username taken | username={}", request.username());
                throw new AuthException("Username is already taken.");
            }

            User user = User.builder()
                    .username(request.username())
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .roles(Set.of("USER"))
                    .build();

            userRepository.save(user);
            registerSuccessCounter.increment();

            log.info("Register success | userId={} email={}", user.getId(), user.getEmail());
            return buildAuthResponse(user);
        });
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        return loginTimer.record(() -> {
            log.info("Login attempt | email={} corrId={}",
                    request.email(), MDC.get("correlationId"));

            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.email(), request.password())
                );
            } catch (BadCredentialsException ex) {
                loginFailureCounter.increment();
                log.warn("Login failed — bad credentials | email={}", request.email());
                throw new AuthException("Invalid email or password.");
            } catch (DisabledException ex) {
                loginFailureCounter.increment();
                log.warn("Login failed — account disabled | email={}", request.email());
                throw new AuthException("Account is disabled.");
            }

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new AuthException("User not found."));

            loginSuccessCounter.increment();
            MDC.put("userId", String.valueOf(user.getId()));
            log.info("Login success | userId={} email={}", user.getId(), user.getEmail());

            return buildAuthResponse(user);
        });
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.debug("Token refresh attempt | corrId={}", MDC.get("correlationId"));

        RefreshToken refreshToken = refreshTokenService.findByToken(request.refreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        tokenRefreshCounter.increment();
        log.info("Token refreshed | userId={} email={}", user.getId(), user.getEmail());

        return AuthResponse.of(
                newAccessToken,
                refreshToken.getToken(),
                jwtService.getExpirationMs(),
                toUserResponse(user)
        );
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String email) {
        log.info("Logout | email={} corrId={}", email, MDC.get("correlationId"));
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    refreshTokenService.revokeByUser(user);
                    logoutCounter.increment();
                    log.info("Logout success | userId={} email={}", user.getId(), email);
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getExpirationMs(),
                toUserResponse(user)
        );
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(), user.getUsername(),
                user.getEmail(), user.getRoles()
        );
    }
}
