package com.library.lms.repository;

import com.library.lms.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberCode(String memberCode);
    Optional<Member> findByUser_UserId(Long userId);
    boolean existsByMemberCode(String memberCode);

    @Query("SELECT m FROM Member m WHERE " +
           "LOWER(m.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(m.memberCode) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Member> search(String q, Pageable pageable);
}
