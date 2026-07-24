package com.socialmedia.auth.security.oauth2;

import com.socialmedia.auth.domain.DeviceType;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.dto.response.AuthTokenResponse;
import com.socialmedia.auth.repository.UserRepository;
import com.socialmedia.auth.service.AuthService;
import com.socialmedia.common.exception.ResourceNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The OAuth2 dance is browser-redirect based, so unlike every other login path this
 * finishes with a redirect (not a JSON body) to the SPA, which reads the tokens off the
 * query string and stores them exactly as it would from a normal /auth/login response.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final String redirectUri;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, AuthService authService,
            @Value("${app.security.oauth2.authorized-redirect-uri:http://localhost:3000/oauth2/callback}") String redirectUri) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.redirectUri = redirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        UUID userId = UUID.fromString(String.valueOf(oAuth2User.getAttribute(CustomOAuth2UserService.INTERNAL_USER_ID_ATTRIBUTE)));

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));

        String deviceId = requestParam(request, "device_id", "oauth2-" + UUID.randomUUID());
        AuthTokenResponse tokens = authService.issueTokensForUser(user, deviceId, "OAuth2 login", DeviceType.WEB,
                request.getRemoteAddr(), request.getHeader("User-Agent"));

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", encode(tokens.accessToken()))
                .queryParam("refreshToken", encode(tokens.refreshToken()))
                .queryParam("expiresIn", tokens.expiresIn())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String requestParam(HttpServletRequest request, String name, String fallback) {
        String value = request.getParameter(name);
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
