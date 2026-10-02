package com.college.library.security;

import com.college.library.entity.RoleName;
import com.college.library.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** The logged-in user as Spring Security sees it. Authority is ROLE_ADMIN / ROLE_LIBRARIAN / ROLE_MEMBER. */
public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final RoleName role;
    private final boolean enabled;

    public AppUserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole().getName();
        this.enabled = user.isEnabled();
    }

    public Long getId() { return id; }
    public RoleName getRole() { return role; }
    public boolean hasRole(RoleName r) { return role == r; }
    public boolean isStaff() { return role == RoleName.ADMIN || role == RoleName.LIBRARIAN; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
