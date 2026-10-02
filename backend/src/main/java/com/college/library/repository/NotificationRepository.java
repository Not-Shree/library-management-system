package com.college.library.repository;

import com.college.library.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByMember_IdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    Optional<Notification> findByIdAndMember_Id(Long id, Long memberId);

    long countByMember_IdAndReadFalse(Long memberId);

    @Modifying
    @Query("update Notification n set n.read = true where n.member.id = :memberId and n.read = false")
    int markAllRead(@Param("memberId") Long memberId);
}
