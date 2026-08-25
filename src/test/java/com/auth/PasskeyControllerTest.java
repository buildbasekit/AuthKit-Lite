package com.auth;

import com.auth.controllers.PasskeyController;
import com.auth.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class PasskeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCredentialRepository userCredentialRepository;

    @MockitoBean
    private PublicKeyCredentialUserEntityRepository userEntityRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldReturnOkForPasskeys() throws Exception {
        mockMvc.perform(get("/api/users/me/passkeys")
                .with(jwt().jwt(j -> j.subject("testuser"))))
                .andExpect(status().isOk());
    }
}
