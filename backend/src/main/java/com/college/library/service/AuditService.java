package com.college.library.service;

import com.college.library.dto.MiscDtos.AuditLogResponse;
import com.college.library.dto.PageResponse;
import com.college.library.entity.AuditLog;
import com.college.library.mapper.MiscMapper;
import com.college.library.repository.AuditLogRepository;
import com.college.library.repository.SearchSpecs;
import com.college.library.security.AppUserPrincipal;
import com.college.library.security.CurrentUserService;
import com.college.library.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/** Writes and reads the audit trail. Runs inside the caller's transaction, so a failed action leaves no log. */
@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;

    public AuditService(AuditLogRepository repository, UserRepository userRepository, CurrentUserService currentUser) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public void log(String action, String entityType, Long entityId, String details) {
        AuditLog entry = new AuditLog();
        Optional<AppUserPrincipal> p = currentUser.principal();
        entry.setUser(p.map(u -> userRepository.getReferenceById(u.getId())).orElse(null));
        entry.setUsername(p.map(AppUserPrincipal::getUsername).orElse("system"));
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String q, String entityType, LocalDate from, LocalDate to, Pageable pageable) {
        Page<AuditLog> page = repository.findAll(SearchSpecs.auditLogs(q, entityType,
                from != null ? from.atStartOfDay() : null, to != null ? to.plusDays(1).atStartOfDay() : null), pageable);
        return PageResponse.of(page, page.getContent().stream().map(MiscMapper::toAuditResponse).toList());
    }
}
