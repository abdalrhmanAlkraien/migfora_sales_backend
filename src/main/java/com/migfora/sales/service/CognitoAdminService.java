package com.migfora.sales.service;

import com.migfora.sales.dto.AuthDtos.*;
import com.migfora.sales.exception.AuthException;
import com.migfora.sales.exception.PasswordChangeRequiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CognitoAdminService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Value("${aws.cognito.client-secret}")
    private String clientSecret;
    // ── Secret Hash (required when app client has a secret) ───────────────────

    private String computeSecretHash(String username) {
        try {
            String message = username + clientId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    clientSecret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal(message.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SECRET_HASH", e);
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public AuthenticationResultType login(String email, String password) {
        try {
            InitiateAuthResponse response = cognitoClient.initiateAuth(
                    InitiateAuthRequest.builder()
                            .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                            .clientId(clientId)
                            .authParameters(Map.of(
                                    "USERNAME", email,
                                    "PASSWORD", password,
                                    "SECRET_HASH", computeSecretHash(email)
                            ))
                            .build()
            );

            if (response.challengeName() == ChallengeNameType.NEW_PASSWORD_REQUIRED) {
                log.info("New password required | email={}", email);
                throw new PasswordChangeRequiredException(
                        response.session(), "Password change required"
                );
            }

            return response.authenticationResult();

        } catch (PasswordChangeRequiredException ex) {
            throw ex;
        } catch (NotAuthorizedException ex) {
            log.warn("Login failed — bad credentials | email={}", email);
            throw new AuthException("Invalid email or password.");
        } catch (UserNotFoundException ex) {
            log.warn("Login failed — user not found | email={}", email);
            throw new AuthException("Invalid email or password.");
        } catch (UserNotConfirmedException ex) {
            log.warn("Login failed — user not confirmed | email={}", email);
            throw new AuthException("Account is not confirmed.");
        } catch (TooManyRequestsException ex) {
            log.warn("Login failed — too many attempts | email={}", email);
            throw new AuthException("Too many attempts. Please try again later.");
        }
    }

    public UserResponse getUserInfo(String accessToken) {
        try {
            // Get user attributes using the access token
            GetUserResponse response = cognitoClient.getUser(
                    GetUserRequest.builder()
                            .accessToken(accessToken)
                            .build()
            );

            // Map attributes to key/value
            Map<String, String> attrs = response.userAttributes().stream()
                    .collect(Collectors.toMap(
                            AttributeType::name,
                            AttributeType::value
                    ));

            // Get groups for this user
            List<String> groups = cognitoClient.adminListGroupsForUser(
                            AdminListGroupsForUserRequest.builder()
                                    .userPoolId(userPoolId)
                                    .username(response.username())
                                    .build()
                    ).groups().stream()
                    .map(GroupType::groupName)
                    .collect(Collectors.toList());

            boolean isAdmin = groups.contains("admin_group");

            return new UserResponse(
                    attrs.get("sub"),
                    attrs.get("email"),
                    attrs.get("name"),
                    attrs.get("family_name"),
                    groups,
                    isAdmin
            );

        } catch (Exception ex) {
            log.error("Failed to get user info | error={}", ex.getMessage());
            throw new AuthException("Failed to retrieve user information.");
        }
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    public AuthenticationResultType refreshToken(String refreshToken) {
        try {
            InitiateAuthResponse response = cognitoClient.initiateAuth(
                    InitiateAuthRequest.builder()
                            .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                            .clientId(clientId)
                            .authParameters(Map.of(
                                    "REFRESH_TOKEN", refreshToken,
                                    "SECRET_HASH", computeSecretHash(refreshToken)
                            ))
                            .build()
            );
            return response.authenticationResult();
        } catch (NotAuthorizedException ex) {
            throw new AuthException("Refresh token is invalid or expired.");
        }
    }

    // ── Force Change Password ─────────────────────────────────────────────────

    public AuthenticationResultType forceChangePassword(String email,
                                                        String newPassword,
                                                        String session) {
        try {
            RespondToAuthChallengeResponse response = cognitoClient.respondToAuthChallenge(
                    RespondToAuthChallengeRequest.builder()
                            .clientId(clientId)
                            .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                            .session(session)
                            .challengeResponses(Map.of(
                                    "USERNAME", email,
                                    "NEW_PASSWORD", newPassword,
                                    "SECRET_HASH", computeSecretHash(email)
                            ))
                            .build()
            );
            return response.authenticationResult();
        } catch (InvalidPasswordException ex) {
            throw new AuthException("Password does not meet requirements.");
        } catch (ExpiredCodeException ex) {
            throw new AuthException("Session expired. Please login again.");
        }
    }

    // ── Admin: Create User ────────────────────────────────────────────────────

    public void createUser(String email, String name,
                           String familyName, String phoneNumber) {
        try {
            cognitoClient.adminCreateUser(
                    AdminCreateUserRequest.builder()
                            .userPoolId(userPoolId)
                            .username(email)
                            .userAttributes(
                                    AttributeType.builder().name("email").value(email).build(),
                                    AttributeType.builder().name("name").value(name).build(),
                                    AttributeType.builder().name("family_name").value(familyName).build(),
                                    AttributeType.builder().name("phone_number").value(phoneNumber).build(),
                                    AttributeType.builder().name("email_verified").value("true").build()
                            )
                            .desiredDeliveryMediums(DeliveryMediumType.EMAIL)
                            .build()
            );
            log.info("Cognito user created | email={}", email);
        } catch (UsernameExistsException ex) {
            throw new AuthException("A user with this email already exists.");
        }
    }

    // ── Admin: Assign Group ───────────────────────────────────────────────────

    public void assignGroup(String email, String groupName) {
        cognitoClient.adminAddUserToGroup(
                AdminAddUserToGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(email)
                        .groupName(groupName)
                        .build()
        );
        log.info("User assigned to group | email={} group={}", email, groupName);
    }

    // ── Admin: Disable User ───────────────────────────────────────────────────

    public void disableUser(String email) {
        cognitoClient.adminDisableUser(
                AdminDisableUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(email)
                        .build()
        );
        log.info("Cognito user disabled | email={}", email);
    }

    // ── Admin: Get User ───────────────────────────────────────────────────────

    public AdminGetUserResponse getUser(String email) {
        return cognitoClient.adminGetUser(
                AdminGetUserRequest.builder()
                        .userPoolId(userPoolId)
                        .username(email)
                        .build()
        );
    }

}