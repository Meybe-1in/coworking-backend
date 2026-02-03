package com.coworking.service;

import com.coworking.dto.AuthResponse;
import com.coworking.dto.GoogleUserInfo;
import com.coworking.model.Role;
import com.coworking.model.User;
import com.coworking.repository.RoleRepository;
import com.coworking.repository.UserRepository;
import com.coworking.security.JwtUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate = new RestTemplate();

    public AuthResponse authenticate(String accessToken){
        GoogleUserInfo googleUser = getUserInfo(accessToken);

        if (googleUser == null || !Boolean.TRUE.equals(googleUser.getEmail_verified())) {
            throw new RuntimeException("Token de Google inválido");
        }

        User user = userRepository.findByEmail(googleUser.getEmail())
                .orElseGet(() -> registerGoogleUser(googleUser.getEmail(), googleUser.getName()));

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password("")
                        .authorities(
                                user.getRoles().stream()
                                        .map(Role::getName)
                                        .toArray(String[]::new)
                        )
                        .build();

        String token = jwtUtil.generateToken(userDetails, user.getUsername());

        String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");
        return new AuthResponse(token, user.getUsername(), role);
    }

    private GoogleUserInfo getUserInfo(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserInfo> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        GoogleUserInfo.class
                );

        return response.getBody();

    }

    private User registerGoogleUser(String email, String name) {
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Rol USER no existe"));

        User user = new User();
        user.setEmail(email);
        user.setUsername(name);
        user.setEnabled(true); // verifico email
        user.getRoles().add(role);

        return userRepository.save(user);

    }

    /*private GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(List.of(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null){
                throw new RuntimeException("Token de Google inválido");
            }
            return idToken.getPayload();


        } catch (Exception e) {
            throw new RuntimeException("Error validando token Google", e);
        }
    }*/
}
