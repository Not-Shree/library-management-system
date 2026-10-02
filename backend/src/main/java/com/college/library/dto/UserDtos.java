package com.college.library.dto;

import com.college.library.entity.RoleName;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/** Staff (librarian/admin) account management. */
public final class UserDtos {

    private UserDtos() {
    }

    public record CreateStaffRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Pattern(regexp = AuthDtos.PASSWORD_RULE, message = AuthDtos.PASSWORD_MESSAGE) String password,
            @NotNull RoleName role) {
    }

    public record UpdateStaffRequest(
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(max = 120) String fullName,
            @NotNull Boolean enabled) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Pattern(regexp = AuthDtos.PASSWORD_RULE, message = AuthDtos.PASSWORD_MESSAGE) String newPassword) {
    }

    public record UserResponse(Long id, String username, String email, String fullName, String role,
                               boolean enabled, LocalDateTime createdAt) {
    }
}
