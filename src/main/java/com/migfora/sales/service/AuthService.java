package com.migfora.sales.service;

import com.migfora.sales.dto.AuthDtos;
import com.migfora.sales.enitty.RefreshToken;
import com.migfora.sales.enitty.User;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException("Email is already registered.");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new AuthException("Username is already taken.");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of("USER"))
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new AuthException("Invalid email or password.");
        } catch (DisabledException ex) {
            throw new AuthException("Account is disabled.");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException("User not found."));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse refreshToken(AuthDtos.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.refreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newAccessToken = jwtService.generateToken(userDetails);

        return AuthDtos.AuthResponse.of(
                newAccessToken,
                refreshToken.getToken(),
                jwtService.getExpirationMs(),
                toUserResponse(user)
        );
    }

    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email)
                .ifPresent(refreshTokenService::revokeByUser);
    }

    private AuthDtos.AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthDtos.AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getExpirationMs(),
                toUserResponse(user)
        );
    }

    private AuthDtos.UserResponse toUserResponse(User user) {
        return new AuthDtos.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles()
        );
    }
}
