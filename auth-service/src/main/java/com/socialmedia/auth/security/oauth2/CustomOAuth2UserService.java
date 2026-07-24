package com.socialmedia.auth.security.oauth2;

import com.socialmedia.auth.domain.AuthProvider;
import com.socialmedia.auth.domain.User;
import com.socialmedia.auth.event.AuthEventPublisher;
import com.socialmedia.auth.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    public static final String INTERNAL_USER_ID_ATTRIBUTE = "internalUserId";

    private final UserRepository userRepository;
    private final AuthEventPublisher eventPublisher;

    public CustomOAuth2UserService(UserRepository userRepository, AuthEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.from(registrationId, oAuth2User.getAttributes());
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        User user = findOrCreateUser(provider, userInfo);

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put(INTERNAL_USER_ID_ATTRIBUTE, user.getId().toString());

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, nameAttributeKey(registrationId));
    }

    private User findOrCreateUser(AuthProvider provider, OAuth2UserInfo userInfo) {
        return userRepository.findByProviderAndProviderId(provider, userInfo.getProviderId())
                .or(() -> userRepository.findByEmailIgnoreCase(userInfo.getEmail()))
                .map(existing -> {
                    if (existing.getProvider() != provider) {
                        // Local (or other-provider) account signing in with a new provider
                        // for the first time - link it rather than creating a duplicate.
                        existing.setProvider(provider);
                        existing.setProviderId(userInfo.getProviderId());
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    User created = new User(userInfo.getEmail(), null, provider, userInfo.getProviderId());
                    created.setEmailVerified(true); // the OAuth provider already verified it
                    User saved = userRepository.save(created);
                    eventPublisher.publishUserRegistered(saved);
                    return saved;
                });
    }

    private String nameAttributeKey(String registrationId) {
        return "github".equalsIgnoreCase(registrationId) ? "id" : "sub";
    }
}
