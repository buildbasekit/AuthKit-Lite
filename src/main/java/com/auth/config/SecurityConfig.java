package com.auth.config;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.auth.config.PasskeyProperties;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtProperties jwtProperties;
	private final PasskeyProperties passkeyProperties;
	private final UserDetailsService userDetailsService;
	public SecurityConfig(JwtProperties jwtProperties, PasskeyProperties passkeyProperties,
						  UserDetailsService userDetailsService) {
		this.jwtProperties = jwtProperties;
		this.passkeyProperties = passkeyProperties;
		this.userDetailsService = userDetailsService;
	}

	@Bean
	public org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository userCredentialRepository(org.springframework.jdbc.core.JdbcOperations jdbcOperations) {
		return new org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository(jdbcOperations);
	}

	@Bean
	public org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository userEntityRepository(org.springframework.jdbc.core.JdbcOperations jdbcOperations) {
		return new org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository(jdbcOperations);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(authProvider);
	}

	@Bean
	@org.springframework.core.annotation.Order(1)
	public SecurityFilterChain webAuthnFilterChain(HttpSecurity http,
												   com.auth.security.webauthn.WebAuthnAuthenticationSuccessHandler webAuthnAuthenticationSuccessHandler) throws Exception {
		http.securityMatcher("/webauthn/**");
		
		if (passkeyProperties.enabled()) {
			http.csrf(csrf -> csrf.csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse()))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.webAuthn(webAuthn -> webAuthn
						.rpName(passkeyProperties.rpName())
						.rpId(passkeyProperties.rpId())
						.allowedOrigins(passkeyProperties.allowedOrigins().toArray(new String[0]))
						.withObjectPostProcessor(new org.springframework.security.config.ObjectPostProcessor<org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter>() {
							@Override
							public <O extends org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter> O postProcess(O filter) {
								filter.setAuthenticationSuccessHandler(webAuthnAuthenticationSuccessHandler);
								return filter;
							}
						})
				);
		} else {
			http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
		}

		return http.build();
	}

	@Bean
	@org.springframework.core.annotation.Order(2)
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/**")
			.csrf(csrf -> csrf.disable())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
					.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
					.decoder(jwtDecoder())
					.jwtAuthenticationConverter(jwtAuthenticationConverter())));

		return http.build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		SecretKeySpec secretKey = new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();

		OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefaultWithIssuer(jwtProperties.issuer());
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
				JwtClaimNames.AUD, aud -> aud != null && aud.contains(jwtProperties.audience()));
		
		jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidators, audienceValidator));
		
		return jwtDecoder;
	}

	@Bean
	public JwtEncoder jwtEncoder() {
		JWKSource<SecurityContext> jwkSource = new ImmutableSecret<>(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
		grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
		grantedAuthoritiesConverter.setAuthorityPrefix(""); // DB roles are already prefixed with ROLE_

		JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
		jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
		return jwtAuthenticationConverter;
	}

}
