package com.college.library.repository;

import com.college.library.entity.Member;
import com.college.library.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    Optional<Member> findByUser_Id(Long userId);

    Optional<Member> findByMemberCodeIgnoreCase(String memberCode);

    boolean existsByMemberCodeIgnoreCase(String memberCode);

    boolean existsByMemberCodeIgnoreCaseAndIdNot(String memberCode, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByStatus(MemberStatus status);
}
