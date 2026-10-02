package com.college.library.service;

import com.college.library.dto.UserDtos.*;
import com.college.library.entity.RoleName;
import com.college.library.entity.User;
import com.college.library.exception.BusinessRuleException;
import com.college.library.exception.DuplicateResourceException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.mapper.MiscMapper;
import com.college.library.repository.RoleRepository;
import com.college.library.repository.UserRepository;
import com.college.library.security.AppUserPrincipal;
import com.college.library.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin management of librarian / admin accounts, and password resets. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUser;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       CurrentUserService currentUser, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> staff() {
        return userRepository.findByRole_NameInOrderByFullNameAsc(List.of(RoleName.ADMIN, RoleName.LIBRARIAN)).stream()
                .map(MiscMapper::toUserResponse).toList();
    }

    @Transactional
    public UserResponse createStaff(CreateStaffRequest req) {
        if (req.role() == RoleName.MEMBER) {
            throw new BusinessRuleException("INVALID_ROLE", "Members are created from the Members page");
        }
        if (userRepository.existsByUsernameIgnoreCase(req.username())) {
            throw new DuplicateResourceException("DUPLICATE_USERNAME", "Username \"" + req.username() + "\" is taken");
        }
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new DuplicateResourceException("DUPLICATE_EMAIL", "Email " + req.email() + " is already used");
        }
        User u = new User();
        u.setUsername(req.username().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setFullName(req.fullName().trim());
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setRole(roleRepository.findByName(req.role()).orElseThrow());
        u.setEnabled(true);
        userRepository.save(u);
        auditService.log("CREATE_STAFF", "User", u.getId(), u.getUsername() + " as " + req.role());
        return MiscMapper.toUserResponse(u);
    }

    @Transactional
    public UserResponse updateStaff(Long id, UpdateStaffRequest req) {
        User u = find(id);
        if (u.getRole().getName() == RoleName.MEMBER) {
            throw new BusinessRuleException("INVALID_ROLE", "Member accounts are managed from the Members page");
        }
        if (!req.enabled() && u.getId().equals(currentUser.requirePrincipal().getId())) {
            throw new BusinessRuleException("INVALID_OPERATION", "You cannot disable your own account");
        }
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(req.email(), id)) {
            throw new DuplicateResourceException("DUPLICATE_EMAIL", "Email " + req.email() + " is already used");
        }
        u.setEmail(req.email().trim().toLowerCase());
        u.setFullName(req.fullName().trim());
        u.setEnabled(req.enabled());
        auditService.log("UPDATE_STAFF", "User", id, u.getUsername() + (req.enabled() ? " (enabled)" : " (disabled)"));
        return MiscMapper.toUserResponse(u);
    }

    /**
     * Replaces the "forgot password" email flow: an admin may reset any password,
     * a librarian may reset member passwords only.
     */
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req) {
        User u = find(id);
        AppUserPrincipal caller = currentUser.requirePrincipal();
        if (caller.hasRole(RoleName.LIBRARIAN) && u.getRole().getName() != RoleName.MEMBER) {
            throw new BusinessRuleException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Librarians can only reset member passwords");
        }
        u.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        auditService.log("RESET_PASSWORD", "User", id, "Password reset for " + u.getUsername());
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
