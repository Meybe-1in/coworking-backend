package com.coworking.util;

import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

//@Profile("dev")
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.findByName("ROLE_USER").isEmpty()){
            roleRepository.save(new Role("ROLE_USER"));
        }
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(new Role("ROLE_ADMIN"));
        }

        // CREAR ADMIN POR DEFECTO
        if (userRepository.findByUsername("admin").isEmpty()
        ) {

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN no encontrado"));


            User admin = new User();

            admin.setUsername("admin");
            admin.setEmail("admin@coworking.com");
            admin.setPassword(passwordEncoder.encode("Admin123*"));
            admin.setEnabled(true);
            admin.setEmailVerified(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setRoles(Set.of(adminRole));

            userRepository.save(admin);

            System.out.println("ADMIN POR DEFECTO CREADO");
        }
    }
}
