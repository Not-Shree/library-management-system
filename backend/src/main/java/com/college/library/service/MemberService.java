package com.college.library.service;

import com.college.library.dto.MemberDtos.*;
import com.college.library.dto.PageResponse;
import com.college.library.entity.*;
import com.college.library.exception.BusinessRuleException;
import com.college.library.exception.DuplicateResourceException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.MemberMapper;
import com.college.library.repository.*;
import com.college.library.security.CurrentUserService;
import com.college.library.util.TextUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BorrowingRepository borrowingRepository;
    private final FineRepository fineRepository;
    private final SettingsService settingsService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final CurrentUserService currentUser;

    public MemberService(MemberRepository memberRepository, UserRepository userRepository, RoleRepository roleRepository,
                         BorrowingRepository borrowingRepository, FineRepository fineRepository,
                         SettingsService settingsService, PasswordEncoder passwordEncoder, AuditService auditService,
                         CurrentUserService currentUser) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.borrowingRepository = borrowingRepository;
        this.fineRepository = fineRepository;
        this.settingsService = settingsService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<MemberResponse> search(String q, MemberStatus status, Pageable pageable) {
        Page<Member> page = memberRepository.findAll(SearchSpecs.members(q, status), pageable);
        return PageResponse.of(page, toResponses(page.getContent()));
    }

    @Transactional(readOnly = true)
    public MemberResponse get(Long id) {
        return toResponses(List.of(find(id))).get(0);
    }

    @Transactional
    public MemberResponse create(MemberRequest req) {
        checkUnique(req.memberCode(), req.email(), null);
        Member m = new Member();
        apply(m, req);
        m.setMembershipDate(req.membershipDate() != null ? req.membershipDate() : LocalDate.now());
        m.setStatus(MemberStatus.ACTIVE);
        if (TextUtils.hasText(req.username())) {
            if (!TextUtils.hasText(req.password())) {
                throw new BusinessRuleException("PASSWORD_REQUIRED", "Enter a password for the member's login");
            }
            m.setUser(createMemberUser(req.username(), req.password(), m.getEmail(), m.getFullName()));
        }
        memberRepository.save(m);
        auditService.log("CREATE_MEMBER", "Member", m.getId(), m.getMemberCode() + " " + m.getFullName());
        return get(m.getId());
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest req) {
        Member m = find(id);
        checkUnique(req.memberCode(), req.email(), id);
        apply(m, req);
        if (req.membershipDate() != null) m.setMembershipDate(req.membershipDate());
        auditService.log("UPDATE_MEMBER", "Member", id, m.getMemberCode());
        return get(id);
    }

    @Transactional
    public MemberResponse setStatus(Long id, MemberStatus status) {
        Member m = find(id);
        m.setStatus(status);
        if (m.getUser() != null) m.getUser().setEnabled(status == MemberStatus.ACTIVE);
        auditService.log(status == MemberStatus.ACTIVE ? "ACTIVATE_MEMBER" : "DEACTIVATE_MEMBER", "Member", id, m.getMemberCode());
        return get(id);
    }

    /** Members with any borrowing history cannot be deleted (history must be kept); deactivate them instead. */
    @Transactional
    public void delete(Long id) {
        Member m = find(id);
        if (borrowingRepository.existsByMember_Id(id)) {
            throw new BusinessRuleException(HttpStatus.CONFLICT, "MEMBER_HAS_HISTORY",
                    "This member has borrowing history, so the record is kept. Deactivate the member instead.");
        }
        User user = m.getUser();
        memberRepository.delete(m);
        if (user != null) userRepository.delete(user);
        auditService.log("DELETE_MEMBER", "Member", id, m.getMemberCode());
    }

    /** Give an existing member a portal login. */
    @Transactional
    public MemberResponse createLogin(Long id, CreateLoginRequest req) {
        Member m = find(id);
        if (m.getUser() != null) {
            throw new BusinessRuleException("LOGIN_EXISTS", "This member already has the login \"" + m.getUser().getUsername() + "\"");
        }
        m.setUser(createMemberUser(req.username(), req.password(), m.getEmail(), m.getFullName()));
        auditService.log("CREATE_MEMBER_LOGIN", "Member", id, "username " + req.username());
        return get(id);
    }

    // ---------------------------------------------------------------- self-service
    @Transactional(readOnly = true)
    public MemberResponse myProfile() {
        return get(currentUser.requireCurrentMember().getId());
    }

    @Transactional
    public MemberResponse updateMyProfile(ProfileUpdateRequest req) {
        Member m = currentUser.requireCurrentMember();
        m.setPhone(TextUtils.clean(req.phone()));
        m.setAddress(TextUtils.clean(req.address()));
        return get(m.getId());
    }

    // ---------------------------------------------------------------- helpers
    User createMemberUser(String username, String password, String email, String fullName) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateResourceException("DUPLICATE_USERNAME", "Username \"" + username + "\" is taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("DUPLICATE_EMAIL", "An account with email " + email + " already exists");
        }
        User u = new User();
        u.setUsername(username.trim());
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(roleRepository.findByName(RoleName.MEMBER).orElseThrow());
        u.setEnabled(true);
        return userRepository.save(u);
    }

    private void checkUnique(String code, String email, Long id) {
        boolean codeTaken = id == null ? memberRepository.existsByMemberCodeIgnoreCase(code.trim())
                : memberRepository.existsByMemberCodeIgnoreCaseAndIdNot(code.trim(), id);
        if (codeTaken) throw new DuplicateResourceException("DUPLICATE_MEMBER_CODE", "Member ID " + code + " is already registered");
        boolean emailTaken = id == null ? memberRepository.existsByEmailIgnoreCase(email.trim())
                : memberRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), id);
        if (emailTaken) throw new DuplicateResourceException("DUPLICATE_EMAIL", "Email " + email + " is already registered");
    }

    private static void apply(Member m, MemberRequest req) {
        m.setMemberCode(req.memberCode().trim().toUpperCase());
        m.setFullName(req.fullName().trim());
        m.setEmail(req.email().trim().toLowerCase());
        m.setPhone(TextUtils.clean(req.phone()));
        m.setDepartment(TextUtils.clean(req.department()));
        m.setCourse(TextUtils.clean(req.course()));
        m.setYearOfStudy(req.yearOfStudy());
        m.setAddress(TextUtils.clean(req.address()));
        m.setMaxBooksAllowed(req.maxBooksAllowed());
    }

    private Member find(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Member", id));
    }

    /** Borrowed-book counts and outstanding fines for a page of members, in two queries. */
    private List<MemberResponse> toResponses(List<Member> members) {
        if (members.isEmpty()) return List.of();
        List<Long> ids = members.stream().map(Member::getId).toList();
        Map<Long, Long> borrowed = new HashMap<>();
        for (Object[] r : borrowingRepository.countByMemberIds(ids, BorrowingStatus.BORROWED)) {
            borrowed.put((Long) r[0], ((Number) r[1]).longValue());
        }
        Map<Long, BigDecimal> owed = new HashMap<>();
        for (Object[] r : fineRepository.outstandingByMemberIds(ids)) {
            owed.put((Long) r[0], (BigDecimal) r[1]);
        }
        int defaultLimit = settingsService.current().getMaxBooksPerMember();
        return members.stream().map(m -> MemberMapper.toResponse(m, defaultLimit,
                borrowed.getOrDefault(m.getId(), 0L), owed.get(m.getId()))).toList();
    }
}
