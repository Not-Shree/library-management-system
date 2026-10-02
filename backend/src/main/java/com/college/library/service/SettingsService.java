package com.college.library.service;

import com.college.library.dto.MiscDtos.SettingsDto;
import com.college.library.entity.LibrarySettings;
import com.college.library.exception.BusinessRuleException;
import com.college.library.mapper.MiscMapper;
import com.college.library.repository.LibrarySettingsRepository;
import com.college.library.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and updates the single library_settings row. */
@Service
public class SettingsService {

    private final LibrarySettingsRepository repository;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public SettingsService(LibrarySettingsRepository repository, AuditService auditService, CurrentUserService currentUser) {
        this.repository = repository;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    /** The current rules. The row is created by database/schema.sql. */
    @Transactional(readOnly = true)
    public LibrarySettings current() {
        return repository.findById(LibrarySettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("library_settings row is missing - run database/schema.sql"));
    }

    @Transactional(readOnly = true)
    public SettingsDto get() {
        return MiscMapper.toSettingsDto(current());
    }

    @Transactional
    public SettingsDto update(SettingsDto dto) {
        if (dto.maxFinePerBook().compareTo(dto.finePerDay()) < 0) {
            throw new BusinessRuleException("INVALID_SETTINGS", "Maximum fine per book cannot be less than the fine per day");
        }
        LibrarySettings s = current();
        String before = describe(s);
        s.setFinePerDay(dto.finePerDay());
        s.setMaxFinePerBook(dto.maxFinePerBook());
        s.setGracePeriodDays(dto.gracePeriodDays());
        s.setMaxBooksPerMember(dto.maxBooksPerMember());
        s.setLoanPeriodDays(dto.loanPeriodDays());
        s.setMaxRenewals(dto.maxRenewals());
        s.setRenewalPeriodDays(dto.renewalPeriodDays());
        s.setAllowRenewalWhenOverdue(dto.allowRenewalWhenOverdue());
        s.setBlockIssueOnUnpaidFines(dto.blockIssueOnUnpaidFines());
        s.setFineBlockThreshold(dto.fineBlockThreshold());
        s.setReservationHoldDays(dto.reservationHoldDays());
        s.setDueReminderDays(dto.dueReminderDays());
        s.setUpdatedBy(currentUser.principal().map(p -> p.getUsername()).orElse("system"));
        repository.saveAndFlush(s);
        auditService.log("UPDATE_SETTINGS", "LibrarySettings", s.getId(), "Before: " + before + " | After: " + describe(s));
        return MiscMapper.toSettingsDto(s);
    }

    private static String describe(LibrarySettings s) {
        return "fine/day=" + s.getFinePerDay() + ", maxFine=" + s.getMaxFinePerBook() + ", grace=" + s.getGracePeriodDays()
                + ", maxBooks=" + s.getMaxBooksPerMember() + ", loan=" + s.getLoanPeriodDays()
                + ", renewals=" + s.getMaxRenewals() + "x" + s.getRenewalPeriodDays() + "d";
    }
}
