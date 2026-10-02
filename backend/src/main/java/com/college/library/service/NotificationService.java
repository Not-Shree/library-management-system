package com.college.library.service;

import com.college.library.dto.MiscDtos.NotificationResponse;
import com.college.library.dto.PageResponse;
import com.college.library.entity.*;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.MiscMapper;
import com.college.library.repository.BorrowingRepository;
import com.college.library.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** In-app notifications. No external email/SMS service is needed. */
@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final BorrowingRepository borrowingRepository;
    private final SettingsService settingsService;

    public NotificationService(NotificationRepository repository, BorrowingRepository borrowingRepository,
                               SettingsService settingsService) {
        this.repository = repository;
        this.borrowingRepository = borrowingRepository;
        this.settingsService = settingsService;
    }

    @Transactional
    public void notify(Member member, NotificationType type, String title, String message) {
        Notification n = new Notification();
        n.setMember(member);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message.length() > 500 ? message.substring(0, 500) : message);
        repository.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> forMember(Long memberId, Pageable pageable) {
        Page<Notification> page = repository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable);
        return PageResponse.of(page, page.getContent().stream().map(MiscMapper::toNotificationResponse).toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long memberId) {
        return repository.countByMember_IdAndReadFalse(memberId);
    }

    @Transactional
    public void markRead(Long memberId, Long notificationId) {
        Notification n = repository.findByIdAndMember_Id(notificationId, memberId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        n.setRead(true);
    }

    @Transactional
    public int markAllRead(Long memberId) {
        return repository.markAllRead(memberId);
    }

    /** "Due soon" reminders, sent once per loan (and again after a renewal). */
    @Transactional
    public int sendDueReminders() {
        LocalDate today = LocalDate.now();
        int days = settingsService.current().getDueReminderDays();
        List<Borrowing> dueSoon = borrowingRepository.findByStatusAndDueReminderSentFalseAndDueDateBetween(
                BorrowingStatus.BORROWED, today, today.plusDays(days));
        for (Borrowing b : dueSoon) {
            notify(b.getMember(), NotificationType.DUE_SOON, "Book due soon",
                    "\"" + b.getBookCopy().getBook().getTitle() + "\" is due on " + Formats.date(b.getDueDate())
                            + ". Return or renew it to avoid a fine.");
            b.setDueReminderSent(true);
        }
        return dueSoon.size();
    }

    /** One overdue notice per loan. */
    @Transactional
    public int sendOverdueNotices() {
        LocalDate today = LocalDate.now();
        LibrarySettings s = settingsService.current();
        List<Borrowing> overdue = borrowingRepository.findByStatusAndOverdueNoticeSentFalseAndDueDateBefore(
                BorrowingStatus.BORROWED, today);
        for (Borrowing b : overdue) {
            notify(b.getMember(), NotificationType.OVERDUE, "Book overdue",
                    "\"" + b.getBookCopy().getBook().getTitle() + "\" was due on " + Formats.date(b.getDueDate())
                            + ". A fine of " + Formats.money(s.getFinePerDay()) + " per day is adding up.");
            b.setOverdueNoticeSent(true);
        }
        return overdue.size();
    }
}
