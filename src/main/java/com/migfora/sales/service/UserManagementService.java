package com.migfora.sales.service;

import com.migfora.sales.dto.UserDtos.*;
import com.migfora.sales.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 31/05/2026
 * @Time: 3:38 PM
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {


    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    // ── Get all users ─────────────────────────────────────────────────────────

    public UserListResponse getAllUsers(String paginationToken, int limit) {
        try {
            ListUsersRequest.Builder requestBuilder = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .limit(limit);

            if (paginationToken != null && !paginationToken.isBlank()) {
                requestBuilder.paginationToken(paginationToken);
            }

            ListUsersResponse response = cognitoClient.listUsers(requestBuilder.build());

            List<UserDetailResponse> users = response.users().stream()
                    .map(this::toUserDetailResponse)
                    .collect(Collectors.toList());

            log.info("Listed {} users", users.size());

            return new UserListResponse(
                    users,
                    response.paginationToken(),
                    users.size()
            );

        } catch (Exception ex) {
            log.error("Failed to list users | error={}", ex.getMessage());
            throw new AuthException("Failed to retrieve users.");
        }
    }

    // ── Get user by sub (Cognito username) ────────────────────────────────────

    public UserDetailResponse getUserBySub(String sub) {
        try {
            AdminGetUserResponse response = cognitoClient.adminGetUser(
                    AdminGetUserRequest.builder()
                            .userPoolId(userPoolId)
                            .username(sub)
                            .build()
            );

            // Get groups
            List<String> groups = getUserGroups(sub);
            boolean isAdmin = groups.contains("admin_group");

            Map<String, String> attrs = response.userAttributes().stream()
                    .collect(Collectors.toMap(
                            AttributeType::name,
                            AttributeType::value
                    ));

            return new UserDetailResponse(
                    attrs.getOrDefault("sub", sub),
                    attrs.getOrDefault("email", ""),
                    attrs.getOrDefault("name", ""),
                    attrs.getOrDefault("family_name", ""),
                    attrs.getOrDefault("phone_number", ""),
                    Boolean.parseBoolean(attrs.getOrDefault("email_verified", "false")),
                    Boolean.parseBoolean(attrs.getOrDefault("phone_number_verified", "false")),
                    response.enabled(),
                    response.userStatusAsString(),
                    groups,
                    isAdmin,
                    toLocalDateTime(response.userCreateDate().toString()),
                    toLocalDateTime(response.userLastModifiedDate().toString())
            );

        } catch (UserNotFoundException ex) {
            throw new AuthException("User not found.");
        } catch (Exception ex) {
            log.error("Failed to get user | sub={} error={}", sub, ex.getMessage());
            throw new AuthException("Failed to retrieve user.");
        }
    }

    // ── Update user attributes ────────────────────────────────────────────────

    public UserDetailResponse updateUser(String sub, UpdateUserRequest request,
                                         String updatedBy) {
        try {
            List<AttributeType> attributes = new ArrayList<>();

            if (request.name() != null)
                attributes.add(AttributeType.builder()
                        .name("name").value(request.name()).build());

            if (request.familyName() != null)
                attributes.add(AttributeType.builder()
                        .name("family_name").value(request.familyName()).build());

            if (request.phoneNumber() != null)
                attributes.add(AttributeType.builder()
                        .name("phone_number").value(request.phoneNumber()).build());

            if (!attributes.isEmpty()) {
                cognitoClient.adminUpdateUserAttributes(
                        AdminUpdateUserAttributesRequest.builder()
                                .userPoolId(userPoolId)
                                .username(sub)
                                .userAttributes(attributes)
                                .build()
                );
            }

            log.info("User updated | sub={} by={}", sub, updatedBy);
            return getUserBySub(sub);

        } catch (UserNotFoundException ex) {
            throw new AuthException("User not found.");
        } catch (Exception ex) {
            log.error("Failed to update user | sub={} error={}", sub, ex.getMessage());
            throw new AuthException("Failed to update user.");
        }
    }

    // ── Enable user ───────────────────────────────────────────────────────────

    public void enableUser(String sub, String actionBy) {
        try {
            cognitoClient.adminEnableUser(
                    AdminEnableUserRequest.builder()
                            .userPoolId(userPoolId)
                            .username(sub)
                            .build()
            );
            log.info("User enabled | sub={} by={}", sub, actionBy);
        } catch (UserNotFoundException ex) {
            throw new AuthException("User not found.");
        }
    }

    // ── Disable user ──────────────────────────────────────────────────────────

    public void disableUser(String sub, String actionBy) {
        try {
            cognitoClient.adminDisableUser(
                    AdminDisableUserRequest.builder()
                            .userPoolId(userPoolId)
                            .username(sub)
                            .build()
            );
            log.info("User disabled | sub={} by={}", sub, actionBy);
        } catch (UserNotFoundException ex) {
            throw new AuthException("User not found.");
        }
    }

    // ── Delete user ───────────────────────────────────────────────────────────

    public void deleteUser(String sub, String deletedBy) {
        try {
            cognitoClient.adminDeleteUser(
                    AdminDeleteUserRequest.builder()
                            .userPoolId(userPoolId)
                            .username(sub)
                            .build()
            );
            log.info("User deleted | sub={} by={}", sub, deletedBy);
        } catch (UserNotFoundException ex) {
            throw new AuthException("User not found.");
        }
    }

    // ── Reset password ────────────────────────────────────────────────────────

    public void resetPassword(String sub, String actionBy) {
        try {
            cognitoClient.adminResetUserPassword(
                    AdminResetUserPasswordRequest.builder()
                            .userPoolId(userPoolId)
                            .username(sub)
                            .build()
            );
            log.info("Password reset triggered | sub={} by={}", sub, actionBy);
        } catch (UserNotFoundException ex) {
            throw new AuthException("User not found.");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<String> getUserGroups(String sub) {
        return cognitoClient.adminListGroupsForUser(
                        AdminListGroupsForUserRequest.builder()
                                .userPoolId(userPoolId)
                                .username(sub)
                                .build()
                ).groups().stream()
                .map(GroupType::groupName)
                .collect(Collectors.toList());
    }

    private UserDetailResponse toUserDetailResponse(UserType user) {
        Map<String, String> attrs = user.attributes().stream()
                .collect(Collectors.toMap(
                        AttributeType::name,
                        AttributeType::value
                ));

        String sub = attrs.getOrDefault("sub", user.username());
        List<String> groups = getUserGroups(user.username());
        boolean isAdmin = groups.contains("admin_group");

        return new UserDetailResponse(
                sub,
                attrs.getOrDefault("email", ""),
                attrs.getOrDefault("name", ""),
                attrs.getOrDefault("family_name", ""),
                attrs.getOrDefault("phone_number", ""),
                Boolean.parseBoolean(attrs.getOrDefault("email_verified", "false")),
                Boolean.parseBoolean(attrs.getOrDefault("phone_number_verified", "false")),
                user.enabled(),
                user.userStatusAsString(),
                groups,
                isAdmin,
                LocalDateTime.ofInstant(user.userCreateDate(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(user.userLastModifiedDate(), ZoneId.systemDefault())
        );
    }

    private LocalDateTime toLocalDateTime(String instant) {
        try {
            return LocalDateTime.ofInstant(
                    java.time.Instant.parse(instant),
                    ZoneId.systemDefault()
            );
        } catch (Exception e) {
            return null;
        }
    }
}
