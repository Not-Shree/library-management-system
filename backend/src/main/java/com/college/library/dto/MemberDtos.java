package com.college.library.dto;

import com.college.library.entity.MemberStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class MemberDtos {

    private MemberDtos() {
    }

    public record MemberRequest(
            @NotBlank @Size(max = 30) String memberCode,
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 120) String email,
            @Size(max = 20) @Pattern(regexp = "^[0-9+ -]*$", message = "Phone may contain digits, spaces, + and -") String phone,
            @Size(max = 80) String department,
            @Size(max = 80) String course,
            @Min(1) @Max(6) Integer yearOfStudy,
            @Size(max = 255) String address,
            LocalDate membershipDate,
            @Min(1) @Max(20) Integer maxBooksAllowed,
            /* Optional: create a portal login for the member at the same time. */
            @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]*$") String username,
            @Pattern(regexp = "^$|" + AuthDtos.PASSWORD_RULE, message = AuthDtos.PASSWORD_MESSAGE) String password) {
    }

    public record MemberStatusRequest(@NotNull MemberStatus status) {
    }

    public record CreateLoginRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$") String username,
            @NotBlank @Pattern(regexp = AuthDtos.PASSWORD_RULE, message = AuthDtos.PASSWORD_MESSAGE) String password) {
    }

    /** What a member may change on their own profile. */
    public record ProfileUpdateRequest(
            @Size(max = 20) @Pattern(regexp = "^[0-9+ -]*$") String phone,
            @Size(max = 255) String address) {
    }

    public record MemberResponse(Long id, String memberCode, String fullName, String email, String phone,
                                 String department, String course, Integer yearOfStudy, String address,
                                 LocalDate membershipDate, String status, int maxBooksAllowed, boolean customLimit,
                                 long currentBorrowedBooks, BigDecimal outstandingFine,
                                 Long userId, String username) {
    }
}
