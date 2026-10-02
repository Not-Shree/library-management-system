package com.college.library.service;

import com.college.library.dto.AuthDtos.*;
import com.college.library.entity.Member;
import com.college.library.entity.MemberStatus;
import com.college.library.entity.User;
import com.college.library.exception.ApiException;
import com.college.library.exception.DuplicateResourceException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.MiscMapper;
import com.college.library.repository.MemberRepository;
import com.college.library.repository.UserRepository;
import com.college.library.security.AppUserPrincipal;
import com.college.library.security.CurrentUserService;
import com.college.library.security.JwtService;
import com.college.library.util.TextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUser;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, MemberRepository memberRepository, MemberService memberService,
                       PasswordEncoder passwordEncoder, JwtService jwtService, CurrentUserService currentUser,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    /** Accepts a username or an email address. The same message is used for every failure so accounts can't be guessed. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String login = req.username().trim();
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login)
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Invalid username or password"));
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_DISABLED", "This account is disabled. Contact the library.");
        }
        JwtService.IssuedToken token = jwtService.generateToken(new AppUserPrincipal(user));
        Long memberId = memberRepository.findByUser_Id(user.getId()).map(Member::getId).orElse(null);
        return new AuthResponse(token.token(), "Bearer", token.expiresAt(), MiscMapper.toUserInfo(user, memberId));
    }

    /** Self-registration: creates a MEMBER login plus the member record, then logs the user in. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String code = req.memberCode().trim().toUpperCase();
        String email = req.email().trim().toLowerCase();
        if (memberRepository.existsByMemberCodeIgnoreCase(code)) {
            throw new DuplicateResourceException("DUPLICATE_MEMBER_CODE", "Member ID " + code + " is already registered");
        }
        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("DUPLICATE_EMAIL", "Email " + email + " is already registered");
        }
        User user = memberService.createMemberUser(req.username(), req.password(), email, req.fullName().trim());

        Member m = new Member();
        m.setUser(user);
        m.setMemberCode(code);
        m.setFullName(req.fullName().trim());
        m.setEmail(email);
        m.setPhone(TextUtils.clean(req.phone()));
        m.setDepartment(TextUtils.clean(req.department()));
        m.setCourse(TextUtils.clean(req.course()));
        m.setYearOfStudy(req.yearOfStudy());
        m.setMembershipDate(LocalDate.now());
        m.setStatus(MemberStatus.ACTIVE);
        memberRepository.save(m);
        auditService.log("REGISTER", "Member", m.getId(), code + " registered online");

        JwtService.IssuedToken token = jwtService.generateToken(new AppUserPrincipal(user));
        return new AuthResponse(token.token(), "Bearer", token.expiresAt(), MiscMapper.toUserInfo(user, m.getId()));
    }

    @Transactional(readOnly = true)
    public UserInfo me() {
        AppUserPrincipal p = currentUser.requirePrincipal();
        User user = userRepository.findById(p.getId()).orElseThrow(() -> ResourceNotFoundException.of("User", p.getId()));
        Long memberId = memberRepository.findByUser_Id(user.getId()).map(Member::getId).orElse(null);
        return MiscMapper.toUserInfo(user, memberId);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        AppUserPrincipal p = currentUser.requirePrincipal();
        User user = userRepository.findById(p.getId()).orElseThrow(() -> ResourceNotFoundException.of("User", p.getId()));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        auditService.log("CHANGE_PASSWORD", "User", user.getId(), "Password changed by the user");
    }
}
