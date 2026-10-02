package com.college.library.security;

import com.college.library.entity.Member;
import com.college.library.entity.User;
import com.college.library.exception.ApiException;
import com.college.library.exception.ResourceNotFoundException;
import com.college.library.repository.MemberRepository;
import com.college.library.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Helper to find out who is calling the API. */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    public CurrentUserService(UserRepository userRepository, MemberRepository memberRepository) {
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
    }

    /** Empty when called from a background job or an anonymous request. */
    public Optional<AppUserPrincipal> principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public AppUserPrincipal requirePrincipal() {
        return principal().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please log in"));
    }

    /** A lightweight reference to the current user row (for issued_by, recorded_by ...). */
    public User currentUserRef() {
        return principal().map(p -> userRepository.getReferenceById(p.getId())).orElse(null);
    }

    /** The member profile linked to the logged-in account. */
    public Member requireCurrentMember() {
        AppUserPrincipal p = requirePrincipal();
        return memberRepository.findByUser_Id(p.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No member profile is linked to this account"));
    }
}
