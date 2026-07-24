package com.socialmedia.auth.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final UserPrincipal principal;
    private final String jti;

    public JwtAuthenticationToken(UserPrincipal principal, Collection<? extends GrantedAuthority> authorities,
            String jti) {
        super(authorities);
        this.principal = principal;
        this.jti = jti;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public UserPrincipal getPrincipal() {
        return principal;
    }

    public String getJti() {
        return jti;
    }
}
