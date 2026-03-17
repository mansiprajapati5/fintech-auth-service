package com.fintech.system.common.OAuth;

import com.fintech.system.auth.Enum.AuthProvider;
import com.fintech.system.auth.Enum.Role;
import com.fintech.system.auth.Enum.UserStatus;
import com.fintech.system.auth.Model.User;
import com.fintech.system.auth.Repository.UserRepository;
import com.fintech.system.common.JWT.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");
        String providerId = oAuth2User.getAttribute("sub");

        // find existing user or create new one
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .fName(firstName)
                            .lName(lastName)
                            .fullName(firstName + " " + lastName)
                            .oauthProvider(AuthProvider.GOOGLE)
                            .oauthProviderId(providerId)
                            .roles(Set.of(Role.ROLE_USER))
                            .emailVerified(true)
                            .softDelete(false)
                            .userStatus(UserStatus.ACTIVE)
                            .build();
                    return userRepository.save(newUser);
                });

        // generate JWT
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // return token in response
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("{\"accessToken\":\"%s\",\"refreshToken\":\"%s\",\"tokenType\":\"Bearer\"}",
                        token, refreshToken)
        );
    }
}