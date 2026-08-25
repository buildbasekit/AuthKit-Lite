package com.auth.controllers;

import com.auth.dtos.MessageResponse;
import com.auth.entities.User;
import com.auth.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.webauthn.api.Bytes;
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
    private final UserRepository userRepository;

    public PasskeyController(UserCredentialRepository userCredentialRepository,
                             PublicKeyCredentialUserEntityRepository userEntityRepository,
                             UserRepository userRepository) {
        this.userCredentialRepository = userCredentialRepository;
        this.userEntityRepository = userEntityRepository;
        this.userRepository = userRepository;
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

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deletePasskey(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String username = jwt.getSubject();
        
        PublicKeyCredentialUserEntity userEntity = userEntityRepository.findByUsername(username);
        if (userEntity == null) {
            throw new RuntimeException("Passkey not found");
        }

        boolean ownsCredential = userCredentialRepository.findByUserId(userEntity.getId())
                .stream()
                .anyMatch(c -> c.getCredentialId().toBase64UrlString().equals(id));
                
        if (!ownsCredential) {
            throw new RuntimeException("Unauthorized to delete this passkey or passkey not found");
        }

        userCredentialRepository.delete(Bytes.fromBase64(id));
        return ResponseEntity.ok(new MessageResponse("Passkey deleted successfully"));
    }

    public record PasskeyDto(String id, String label, java.time.Instant createdAt, java.time.Instant lastUsedAt) {}
}
