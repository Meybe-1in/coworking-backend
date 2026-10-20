package com.coworking.auth.service;

import com.coworking.auth.dto.AuthResponse;
import com.coworking.auth.dto.GoogleUserInfo;
import com.coworking.role.model.Role;
import com.coworking.user.model.User;
import com.coworking.role.repository.RoleRepository;
import com.coworking.user.repository.UserRepository;
import com.coworking.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    public AuthResponse authenticate(String accessToken, boolean rememberMe) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token de Google no proporcionado"
            );
        }

        GoogleUserInfo googleUser = getUserInfo(accessToken);

        if (googleUser == null
                || googleUser.getEmail() == null
                || googleUser.getEmail().isBlank()
                || !Boolean.TRUE.equals(googleUser.getEmail_verified())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo verificar la identidad con Google"
            );
        }

        String email = googleUser.getEmail().trim();

        /*
         * Si la cuenta ya existe, la reutilizamos.
         * Si no existe, registramos un usuario nuevo.
         */
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> registerGoogleUser(googleUser));

        /*
         * Una cuenta deshabilitada no debe poder entrar, ni siquiera usando Google.
         */
        if (!user.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La cuenta está deshabilitada"
            );
        }

        /*
         * Google ha confirmado que el correo está verificado.
         * Esto permite completar la verificación de una cuenta existente, sin enviar un segundo correo.
         */
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user = userRepository.save(user);
        }

        // Generamos el JWT propio de nuestra plataforma.
        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(
                                user.getRoles()
                                        .stream()
                                        .map(Role::getName)
                                        .toArray(String[]::new)
                        )
                        .build();

        String token = jwtUtil.generateToken(
                userDetails,
                user.getUsername(),
                rememberMe
        );

        String role = user.getRoles()
                .stream()
                .map(Role::getName)
                .filter(name ->
                        name.equals("ROLE_ADMIN") || name.equals(DEFAULT_ROLE)
                )
                .min((first, second) -> {
                    if (first.equals("ROLE_ADMIN")) {
                        return -1;
                    }
                    if (second.equals("ROLE_ADMIN")) {
                        return 1;
                    }
                    return 0;
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "La cuenta no tiene un rol válido asignado"
                ));
        return new AuthResponse(
                token,
                user.getUsername(),
                role
        );
    }

    private GoogleUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleUserInfo> response =
                    restTemplate.exchange(
                            GOOGLE_USER_INFO_URL,
                            HttpMethod.GET,
                            entity,
                            GoogleUserInfo.class
                    );

            return response.getBody();

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "No se pudo validar el token con Google"
            );
        }
    }


    private User registerGoogleUser(GoogleUserInfo googleUser) {
        Role role = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "El rol ROLE_USER no está configurado"
                ));

        User user = new User();
        user.setEmail(googleUser.getEmail().trim());

        String googleName = googleUser.getName();

        if (googleName == null || googleName.isBlank()) {
            String email = googleUser.getEmail().trim();
            int atIndex = email.indexOf('@');

            googleName = atIndex > 0
                    ? email.substring(0, atIndex)
                    : email;
        }

        String username = googleName.trim();

        if (userRepository.existsByUsername(username)) {
            username = username + "-"
                    + UUID.randomUUID().toString().substring(0, 8);
        }

        user.setUsername(username);

        user.setPassword(
                passwordEncoder.encode(UUID.randomUUID().toString())
        );

        user.setEnabled(true);
        user.setEmailVerified(true);
        user.getRoles().add(role);

        return userRepository.save(user);
    }

}
