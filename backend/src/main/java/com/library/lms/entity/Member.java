package com.library.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "member_code", nullable = false, unique = true, length = 30)
    private String memberCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String course;

    @Column(name = "year_of_study", length = 20)
    private String yearOfStudy;

    @Column(length = 255)
    private String address;

    @Column(name = "membership_date", nullable = false)
    @Builder.Default
    private LocalDate membershipDate = LocalDate.now();

    @Column(name = "membership_status", nullable = false, length = 20)
    @Builder.Default
    private String membershipStatus = "ACTIVE"; // ACTIVE, INACTIVE, SUSPENDED

    @Column(name = "max_books_allowed", nullable = false)
    @Builder.Default
    private Integer maxBooksAllowed = 3;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
