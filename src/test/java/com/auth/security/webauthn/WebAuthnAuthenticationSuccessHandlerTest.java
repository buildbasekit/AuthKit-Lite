package com.auth.security.webauthn;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import com.auth.entities.User;
import com.auth.repositories.UserRepository;
import com.auth.security.AuthenticationTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;
import tools.jackson.databind.json.JsonMapper;

class WebAuthnAuthenticationSuccessHandlerTest {

    @Test
    void disabledUserDoesNotReceiveTokens() throws IOException {
        UserRepository userRepository = mock(UserRepository.class);
        AuthenticationTokenService tokenService = mock(AuthenticationTokenService.class);
        JsonMapper jsonMapper = mock(JsonMapper.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        WebAuthnAuthentication authentication = mock(WebAuthnAuthentication.class);

        User disabledUser = new User();
        disabledUser.setEnabled(false);
        when(authentication.getName()).thenReturn("disabled-user");
        when(userRepository.findByUsername("disabled-user")).thenReturn(Optional.of(disabledUser));

        WebAuthnAuthenticationSuccessHandler handler =
                new WebAuthnAuthenticationSuccessHandler(userRepository, tokenService, jsonMapper);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN, "User account is disabled");
        verifyNoInteractions(tokenService, jsonMapper);
    }
}
