package com.college.library.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily housekeeping: due-soon reminders, overdue notices and expiring uncollected holds.
 * Also runs once at startup so the demo data produces notifications straight away.
 */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final NotificationService notificationService;
    private final ReservationService reservationService;

    public ScheduledJobs(NotificationService notificationService, ReservationService reservationService) {
        this.notificationService = notificationService;
        this.reservationService = reservationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        runDailyJobs();
    }

    @Scheduled(cron = "0 0 8 * * *")  // every day at 08:00
    public void runDailyJobs() {
        try {
            int reminders = notificationService.sendDueReminders();
            int overdue = notificationService.sendOverdueNotices();
            int expired = reservationService.expireUncollectedHolds();
            log.info("Daily jobs: {} due reminders, {} overdue notices, {} expired holds", reminders, overdue, expired);
        } catch (Exception e) {
            log.error("Daily jobs failed", e);
        }
    }
}
