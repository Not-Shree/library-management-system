package com.college.library.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;

/** Request/response records for login, registration and password change. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public static final String PASSWORD_RULE = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MESSAGE = "Password needs 8+ characters with at least one letter and one number";

    public record LoginRequest(
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Password is required") String password) {
    }

    /** Self-registration for students/staff. Creates a MEMBER login and a member record. */
    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Use letters, numbers, dot, dash or underscore") String username,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String password,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Size(max = 30) String memberCode,
            @Size(max = 20) String phone,
            @Size(max = 80) String department,
            @Size(max = 80) String course,
            @Min(1) @Max(6) Integer yearOfStudy) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_RULE, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record UserInfo(Long id, String username, String fullName, String email, String role, Long memberId) {
    }

    public record AuthResponse(String token, String tokenType, Instant expiresAt, UserInfo user) {
    }
}
