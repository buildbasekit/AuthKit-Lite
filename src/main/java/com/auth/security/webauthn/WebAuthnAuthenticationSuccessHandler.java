package com.auth.security.webauthn;

import com.auth.dtos.TokenResponse;
import com.auth.entities.User;
import com.auth.repositories.UserRepository;
import com.auth.security.AuthenticationTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class WebAuthnAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthenticationTokenService authenticationTokenService;
    private final JsonMapper jsonMapper;

    public WebAuthnAuthenticationSuccessHandler(UserRepository userRepository, AuthenticationTokenService authenticationTokenService, JsonMapper jsonMapper) {
        this.userRepository = userRepository;
        this.authenticationTokenService = authenticationTokenService;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        if (!(authentication instanceof WebAuthnAuthentication webAuthnAuth)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authentication type");
            return;
        }

        // WebAuthn principal is typically a PublicKeyCredentialUserEntity or similar string/bytes
        // In our JpaPublicKeyCredentialUserEntityRepository, we set name to username.
        String username = webAuthnAuth.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found after WebAuthn authentication"));

        TokenResponse tokenResponse = authenticationTokenService.createTokenResponse(user);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getWriter(), tokenResponse);
    }
}
