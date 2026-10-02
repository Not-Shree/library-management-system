package com.college.library.mapper;

import com.college.library.dto.MemberDtos.MemberResponse;
import com.college.library.entity.Member;

import java.math.BigDecimal;

public final class MemberMapper {

    private MemberMapper() {
    }

    public static MemberResponse toResponse(Member m, int defaultLimit, long borrowedCount, BigDecimal outstanding) {
        boolean custom = m.getMaxBooksAllowed() != null;
        return new MemberResponse(m.getId(), m.getMemberCode(), m.getFullName(), m.getEmail(), m.getPhone(),
                m.getDepartment(), m.getCourse(), m.getYearOfStudy(), m.getAddress(), m.getMembershipDate(),
                m.getStatus().name(), custom ? m.getMaxBooksAllowed() : defaultLimit, custom,
                borrowedCount, outstanding == null ? BigDecimal.ZERO : outstanding,
                m.getUser() != null ? m.getUser().getId() : null,
                m.getUser() != null ? m.getUser().getUsername() : null);
    }
}
