package com.hotelhost.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.hotelhost.backend.entity.User;
import com.hotelhost.backend.entity.UserRole;
import com.hotelhost.backend.repository.UserRepository;

/**
 * Crea el administrador inicial al arrancar, solo si todavía no existe ninguno.
 * Sus datos salen de ADMIN_EMAIL y ADMIN_PASSWORD; deberá cambiar la contraseña en el primer acceso.
 */
@Component
public class AdminInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String adminEmail;
	private final String adminPassword;

	public AdminInitializer(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.admin.email}") String adminEmail,
			@Value("${app.admin.password}") String adminPassword) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminEmail = adminEmail;
		this.adminPassword = adminPassword;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (userRepository.existsByRole(UserRole.ADMIN)) {
			return;
		}
		if (adminEmail.isBlank() || adminPassword.isBlank()) {
			log.warn("No hay ningún administrador. Define ADMIN_EMAIL y ADMIN_PASSWORD para crear el inicial.");
			return;
		}

		User admin = new User();
		admin.setEmail(adminEmail.trim().toLowerCase());
		admin.setPasswordHash(passwordEncoder.encode(adminPassword));
		admin.setFirstName("Administrador");
		admin.setLastName("");
		admin.setRole(UserRole.ADMIN);
		admin.setMustChangePassword(true);
		userRepository.save(admin);

		log.info("Administrador inicial creado: {}", admin.getEmail());
	}
}
