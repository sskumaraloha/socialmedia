package com.socialmedia.auth.security;

import com.socialmedia.auth.domain.User;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security principal backed by our User entity. Used both for the initial
 * password-based AuthenticationManager check and (reconstructed from JWT claims,
 * without a DB hit) by JwtAuthenticationFilter on every subsequent request.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Set<GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountLocked;
    private String deviceId;

    public UserPrincipal(UUID id, String email, String passwordHash, Set<GrantedAuthority> authorities,
            boolean enabled, boolean accountLocked) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
        this.enabled = enabled;
        this.accountLocked = accountLocked;
    }

    public static UserPrincipal from(User user) {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toSet());
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), authorities,
                user.isEnabled(), user.isAccountLocked());
    }

    public UUID getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
