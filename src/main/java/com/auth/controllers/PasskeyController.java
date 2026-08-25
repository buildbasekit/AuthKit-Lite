package com.auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/me/passkeys")
public class PasskeyController {

    private final UserCredentialRepository userCredentialRepository;
    private final PublicKeyCredentialUserEntityRepository userEntityRepository;

    public PasskeyController(UserCredentialRepository userCredentialRepository,
                             PublicKeyCredentialUserEntityRepository userEntityRepository) {
        this.userCredentialRepository = userCredentialRepository;
        this.userEntityRepository = userEntityRepository;
    }

    @GetMapping
    public ResponseEntity<List<PasskeyDto>> listPasskeys(@AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        
        PublicKeyCredentialUserEntity userEntity = userEntityRepository.findByUsername(username);
        if (userEntity == null) {
            return ResponseEntity.ok(List.of());
        }

        List<PasskeyDto> passkeys = userCredentialRepository.findByUserId(userEntity.getId())
                .stream()
                .map(p -> new PasskeyDto(
                        p.getCredentialId().toBase64UrlString(),
                        p.getLabel(),
                        p.getCreated(),
                        p.getLastUsed()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(passkeys);
    }

    public record PasskeyDto(String id, String label, java.time.Instant createdAt, java.time.Instant lastUsedAt) {}
}
