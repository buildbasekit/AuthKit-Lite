package com.auth.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository;
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
	public JdbcUserCredentialRepository userCredentialRepository(org.springframework.jdbc.core.JdbcOperations jdbcOperations) {
		return new JdbcUserCredentialRepository(jdbcOperations);
	}

	@Bean
	public JdbcPublicKeyCredentialUserEntityRepository userEntityRepository(org.springframework.jdbc.core.JdbcOperations jdbcOperations) {
		return new JdbcPublicKeyCredentialUserEntityRepository(jdbcOperations);
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
	@Order(1)
	public SecurityFilterChain webAuthnFilterChain(HttpSecurity http,
											   com.auth.security.webauthn.WebAuthnAuthenticationSuccessHandler webAuthnAuthenticationSuccessHandler) throws Exception {
		http.securityMatcher("/webauthn/**", "/login/webauthn");
		
		if (passkeyProperties.enabled()) {
			http.cors(cors -> { })
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/webauthn/csrf").permitAll()
						.requestMatchers(HttpMethod.POST, "/webauthn/authenticate/options", "/login/webauthn").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
						.decoder(jwtDecoder())
						.jwtAuthenticationConverter(jwtAuthenticationConverter())))
				.webAuthn(webAuthn -> webAuthn
						.rpName(passkeyProperties.rpName())
						.rpId(passkeyProperties.rpId())
						.allowedOrigins(passkeyProperties.allowedOrigins().toArray(new String[0]))
						.disableDefaultRegistrationPage(true)
						.withObjectPostProcessor(new org.springframework.security.config.ObjectPostProcessor<WebAuthnAuthenticationFilter>() {
							@Override
							public <O extends WebAuthnAuthenticationFilter> O postProcess(O filter) {
								filter.setAuthenticationSuccessHandler(webAuthnAuthenticationSuccessHandler);
								return filter;
							}
						})
				);
			http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
		} else {
			http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
		}

		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/**")
			.csrf(csrf -> csrf.disable())
			.cors(cors -> { })
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
	@Order(3)
	public SecurityFilterChain fallbackFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
					.requestMatchers("/api-test", "/api-test/", "/api-test/**").permitAll()
					.requestMatchers(EndpointRequest.toLinks()).permitAll()
					.requestMatchers(EndpointRequest.to("health", "info")).permitAll()
					.anyRequest().denyAll());
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(passkeyProperties.allowedOrigins());
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-XSRF-TOKEN"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		SecretKeySpec secretKey = new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();

		OAuth2TokenValidator<Jwt> defaultValidators = JwtValidators.createDefaultWithIssuer(jwtProperties.issuer());
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(jwtProperties.audience());
		
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
