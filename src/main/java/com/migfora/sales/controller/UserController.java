package com.migfora.sales.controller;

import com.migfora.sales.dto.UserDtos.*;
import com.migfora.sales.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:39 PM
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin only — manage internal users")
@PreAuthorize("hasRole('ADMIN_GROUP')")
public class UserController {


    private final UserManagementService userManagementService;

    @Operation(summary = "Get all users")
    @GetMapping
    public ResponseEntity<UserListResponse> getAllUsers(
            @RequestParam(required = false) String nextToken,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(
                userManagementService.getAllUsers(nextToken, limit)
        );
    }

    @Operation(summary = "Get user by sub")
    @GetMapping("/{sub}")
    public ResponseEntity<UserDetailResponse> getUserBySub(@PathVariable String sub) {
        return ResponseEntity.ok(userManagementService.getUserBySub(sub));
    }

    @Operation(summary = "Update user name, family name, phone")
    @PatchMapping("/{sub}")
    public ResponseEntity<UserDetailResponse> updateUser(
            @PathVariable String sub,
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                userManagementService.updateUser(sub, request, jwt.getSubject())
        );
    }

    @Operation(summary = "Enable user")
    @PatchMapping("/{sub}/enable")
    public ResponseEntity<MessageResponse> enableUser(
            @PathVariable String sub,
            @AuthenticationPrincipal Jwt jwt) {
        userManagementService.enableUser(sub, jwt.getSubject());
        return ResponseEntity.ok(new MessageResponse("User enabled successfully."));
    }

    @Operation(summary = "Disable user")
    @PatchMapping("/{sub}/disable")
    public ResponseEntity<MessageResponse> disableUser(
            @PathVariable String sub,
            @AuthenticationPrincipal Jwt jwt) {
        userManagementService.disableUser(sub, jwt.getSubject());
        return ResponseEntity.ok(new MessageResponse("User disabled successfully."));
    }

    @Operation(summary = "Reset user password — sends temp password to email")
    @PostMapping("/{sub}/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @PathVariable String sub,
            @AuthenticationPrincipal Jwt jwt) {
        userManagementService.resetPassword(sub, jwt.getSubject());
        return ResponseEntity.ok(new MessageResponse("Password reset email sent."));
    }

    @Operation(summary = "Delete user permanently")
    @DeleteMapping("/{sub}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @PathVariable String sub,
            @AuthenticationPrincipal Jwt jwt) {
        userManagementService.deleteUser(sub, jwt.getSubject());
    }
}
