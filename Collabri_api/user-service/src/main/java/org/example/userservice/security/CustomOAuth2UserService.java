package org.example.userservice.security;

import lombok.RequiredArgsConstructor;
import org.example.userservice.entities.User;
import org.example.userservice.repositories.UserRepository;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oauthUser = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauthUser.getAttributes();

        String email;
        String givenName;
        String familyName;

        if ("google".equalsIgnoreCase(registrationId)) {
            email = (String) attributes.get("email");
            givenName = (String) attributes.get("given_name");
            familyName = (String) attributes.get("family_name");
        } else if ("github".equalsIgnoreCase(registrationId)) {
            email = (String) attributes.get("email"); // may be null if private
            Object name = attributes.get("name");
            if (name != null) {
                String n = name.toString();
                if (n.contains(" ")) {
                    givenName = n.substring(0, n.indexOf(' '));
                    familyName = n.substring(n.indexOf(' ') + 1);
                } else {
                    familyName = null;
                    givenName = n;
                }
            } else {
                familyName = null;
                givenName = null;
            }
            // NOTE: for private GitHub email you should call /user/emails with access token (add later)
        } else {
            familyName = null;
            givenName = null;
            email = null;
        }

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_userinfo"), "Email not provided by provider");
        }

        // upsert user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = User.builder()
                    .email(email)
                    .firstname(givenName != null ? givenName : "")
                    .lastname(familyName != null ? familyName : "")
                    .password(null) // OAuth users have no local password
                    .verified(true)  // trust provider
                    .build();
            return userRepository.save(u);
        });

        // update missing fields
        boolean changed = false;
        if ((user.getFirstname() == null || user.getFirstname().isBlank()) && givenName != null) {
            user.setFirstname(givenName);
            changed = true;
        }
        if ((user.getLastname() == null || user.getLastname().isBlank()) && familyName != null) {
            user.setLastname(familyName);
            changed = true;
        }
        if (changed) userRepository.save(user);

        var authorities = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // use the provider's user name attribute (email/sub/id)
        String nameAttr = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(authorities, attributes, nameAttr);
    }
}
