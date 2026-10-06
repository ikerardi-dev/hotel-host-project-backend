package com.hotelhost.backend.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.hotelhost.backend.repository.UserRepository;
import com.hotelhost.backend.security.ApiErrorWriter;
import com.hotelhost.backend.security.PasswordChangeRequiredFilter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * API sin sesión: cada petición debe llevar "Authorization: Bearer <token JWT>".
 * Solo el login y la documentación de Swagger son públicos.
 */
@Configuration
public class SecurityConfig {

	private static final int MIN_SECRET_LENGTH = 32;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository userRepository) throws Exception {
		AuthenticationEntryPoint unauthorized = (request, response, ex) ->
				ApiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Tienes que iniciar sesión");
		AccessDeniedHandler forbidden = (request, response, ex) ->
				ApiErrorWriter.write(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "No tienes permiso para esta acción");

		http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(unauthorized)
						.accessDeniedHandler(forbidden))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(unauthorized)
						.accessDeniedHandler(forbidden))
				// Después de validar el token: bloquea a quien aún tiene la contraseña temporal
				.addFilterAfter(new PasswordChangeRequiredFilter(userRepository), BearerTokenAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecretKey jwtSecretKey(@Value("${app.jwt.secret}") String secret) {
		if (secret.length() < MIN_SECRET_LENGTH) {
			throw new IllegalStateException("JWT_SECRET debe tener al menos " + MIN_SECRET_LENGTH + " caracteres");
		}
		return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	// El claim "role" del token (ADMIN, CLEANING) se convierte en la autoridad ROLE_ADMIN o ROLE_CLEANING
	private JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("role");
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}
}
