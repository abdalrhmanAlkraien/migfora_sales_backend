package com.migfora.sales.controller;

import com.migfora.sales.exception.PasswordChangeRequiredException;
import com.migfora.sales.service.CognitoAdminService;
import com.migfora.sales.dto.AuthDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;

import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 30/05/2026
 * @Time: 11:49 PM
 */

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Cognito-backed auth endpoints")
public class AuthController {

    private final CognitoAdminService cognitoAdminService;

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthenticationResultType result = cognitoAdminService.login(
                    request.email(), request.password()
            );

            UserResponse user = cognitoAdminService.getUserInfo(result.accessToken());

            return ResponseEntity.ok(AuthResponse.fromCognito(result, user));

        } catch (PasswordChangeRequiredException ex) {
            return ResponseEntity.status(428).body(Map.of(
                    "challenge", "NEW_PASSWORD_REQUIRED",
                    "session", ex.getSession(),
                    "message", "Please set a new password"
            ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthenticationResultType result = cognitoAdminService.refreshToken(
                request.refreshToken()
        );
        UserResponse user = cognitoAdminService.getUserInfo(result.accessToken());
        return ResponseEntity.ok(AuthResponse.fromCognito(result, user));
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        AuthenticationResultType result = cognitoAdminService.forceChangePassword(
                request.email(),
                request.newPassword(),
                request.session()
        );
        UserResponse user = cognitoAdminService.getUserInfo(result.accessToken());
        return ResponseEntity.ok(AuthResponse.fromCognito(result, user));
    }

    @Operation(summary = "Get current user info from token")
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN_GROUP', 'SALES')")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(new MeResponse(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsStringList("cognito:groups")
        ));
    }
}
