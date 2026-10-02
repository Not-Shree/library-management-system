package com.college.library.mapper;

import com.college.library.dto.AuthDtos.UserInfo;
import com.college.library.dto.MiscDtos.AuditLogResponse;
import com.college.library.dto.MiscDtos.NotificationResponse;
import com.college.library.dto.MiscDtos.SettingsDto;
import com.college.library.dto.UserDtos.UserResponse;
import com.college.library.entity.*;

public final class MiscMapper {

    private MiscMapper() {
    }

    public static UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(), u.getRole().getName().name(),
                u.isEnabled(), u.getCreatedAt());
    }

    public static UserInfo toUserInfo(User u, Long memberId) {
        return new UserInfo(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole().getName().name(), memberId);
    }

    public static NotificationResponse toNotificationResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }

    public static AuditLogResponse toAuditResponse(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getUsername(), a.getAction(), a.getEntityType(), a.getEntityId(),
                a.getDetails(), a.getCreatedAt());
    }

    public static SettingsDto toSettingsDto(LibrarySettings s) {
        return new SettingsDto(s.getFinePerDay(), s.getMaxFinePerBook(), s.getGracePeriodDays(), s.getMaxBooksPerMember(),
                s.getLoanPeriodDays(), s.getMaxRenewals(), s.getRenewalPeriodDays(), s.isAllowRenewalWhenOverdue(),
                s.isBlockIssueOnUnpaidFines(), s.getFineBlockThreshold(), s.getReservationHoldDays(),
                s.getDueReminderDays(), s.getUpdatedBy(), s.getUpdatedAt());
    }
}
