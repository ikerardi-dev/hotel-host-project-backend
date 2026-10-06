package com.hotelhost.backend.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.hotelhost.backend.entity.User;

/**
 * Genera los tokens JWT de sesión. Claims: sub (id del usuario), role y exp.
 */
@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final Duration expiration;

	public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiration-hours}") long expirationHours) {
		this.jwtEncoder = jwtEncoder;
		this.expiration = Duration.ofHours(expirationHours);
	}

	public IssuedToken generateToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(expiration);

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.claim("role", user.getRole().name())
				.issuedAt(now)
				.expiresAt(expiresAt)
				.build();

		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(token, expiresAt);
	}

	public record IssuedToken(String value, Instant expiresAt) {
	}
}
