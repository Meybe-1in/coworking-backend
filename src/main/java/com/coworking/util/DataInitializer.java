package com.coworking.util;

import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@Profile("docker")
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_USERNAME:}") String adminUsername,
            @Value("${ADMIN_EMAIL:}") String adminEmail,
            @Value("${ADMIN_PASSWORD:}") String adminPassword
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        validateAdminConfiguration();

        // No crear ni modificar cuentas que ya existan.
        boolean usernameExists = userRepository.existsByUsername(adminUsername);
        boolean emailExists = userRepository.existsByEmail(adminEmail);

        if (usernameExists || emailExists) {
            log.warn(
                    "No se creó el administrador local porque el username o email ya está registrado."
            );
            return;
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "ROLE_ADMIN no existe. Verifica que Flyway haya ejecutado las migraciones."
                        )
                );

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        admin.setRoles(Set.of(adminRole));

        userRepository.save(admin);

        log.info("Administrador local creado correctamente.");
    }

    private void validateAdminConfiguration() {
        if (adminUsername == null || adminUsername.isBlank()
                || adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Faltan variables de entorno para inicializar el administrador local: "
                            + "ADMIN_USERNAME, ADMIN_EMAIL y ADMIN_PASSWORD."
            );
        }
    }
}