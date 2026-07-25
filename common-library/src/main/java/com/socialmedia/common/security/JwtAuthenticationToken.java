package com.socialmedia.common.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedPrincipal principal;

    public JwtAuthenticationToken(AuthenticatedPrincipal principal) {
        super(toAuthorities(principal.roles()));
        this.principal = principal;
        setAuthenticated(true);
    }

    private static Collection<GrantedAuthority> toAuthorities(Set<String> roles) {
        return roles.stream().map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        return principal;
    }
}
