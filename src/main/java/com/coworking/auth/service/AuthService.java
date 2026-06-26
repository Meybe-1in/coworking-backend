package com.coworking.auth.service;

import com.coworking.auth.dto.*;
import com.coworking.auth.model.VerificationToken;
import com.coworking.auth.repository.VerificationTokenRepository;
import com.coworking.domain.notification.EmailSender;
import com.coworking.email.service.EmailService;
import com.coworking.email.template.EmailTemplate;
import com.coworking.exception.BadRequestException;
import com.coworking.exception.NotFoundException;
import com.coworking.role.model.Role;
import com.coworking.role.repository.RoleRepository;
import com.coworking.security.JwtUtil;
import com.coworking.user.model.User;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;
    private final EmailSender emailSender;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService;

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                         REGISTER
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    public String register(RegisterRequest request) {

        if (Boolean.FALSE.equals(request.termsAccepted())) {
            throw new BadRequestException("Debe aceptar términos y condiciones");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BadRequestException("Correo ya está registrado");
        }

        if (!request.password().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]{8,}$")) {
            throw new BadRequestException("Contraseña débil");
        }

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new NotFoundException("Rol no encontrado"));

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(role));
        user.setEnabled(false);

        userRepository.save(user);

        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken(
                null,
                token,
                user,
                LocalDateTime.now().plusHours(24)
        );

        tokenRepository.save(verificationToken);

        String link = "http://localhost:8080/auth/verify?token=" + token;

        emailSender.send(
                user.getEmail(),
                "Verifica tu cuenta",
                EmailTemplate.VERIFY_ACCOUNT.getPath(),
                Map.of("link", link)
        );

        return "Registro exitoso. Revisa tu correo para activar tu cuenta.";
    }


    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                         VERIFY
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

    public void verifyAccount(String token, HttpServletResponse response) throws IOException {

        VerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token inválido"));

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            response.sendRedirect("http://localhost:5173/verify-error?reason=expired");
            return;
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        user.setEmailVerified(true);
        userRepository.save(user);

        response.sendRedirect("http://localhost:5173/verify-success");
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                         LOGIN
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas"));

        if (!user.isEnabled()) {
            throw new BadRequestException("EMAIL_NOT_VERIFIED");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(
                userDetails,
                user.getUsername(),
                request.rememberMe()
        );

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        return new AuthResponse(token, user.getUsername(), role);
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                         GET USER
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    public AuthResponse getUser(String token) {

        String email = jwtUtil.extractUsername(token);
        String username = jwtUtil.extractUsernameUi(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        String role = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");

        return new AuthResponse(token, username, role);
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                    RESEND VERIFICATION
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    public String resendVerification(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (user.isEnabled()) {
            throw new BadRequestException("La cuenta ya está verificada");
        }

        // eliminar tokens anteriores
        tokenRepository.deleteAll(
                tokenRepository.findAll()
                        .stream()
                        .filter(t -> t.getUser().equals(user))
                        .toList()
        );

        String token = UUID.randomUUID().toString();

        VerificationToken verificationToken = new VerificationToken(
                null,
                token,
                user,
                LocalDateTime.now().plusHours(24)
        );

        tokenRepository.save(verificationToken);

        String link = "http://localhost:8080/auth/verify?token=" + token;
        emailSender.send(
                user.getEmail(),
                "Verifica tu cuenta",
                EmailTemplate.VERIFY_ACCOUNT.getPath(),
                Map.of("link", link)
        );
        return "Reenvio exitoso. Revisa tu correo para activar tu cuenta.";
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                     REGISTER ADMIN
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    public String registerAdmin(RegisterRequest request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BadRequestException("Correo ya existe");
        }

        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new NotFoundException("Rol ADMIN no encontrado"));

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(roleAdmin));
        user.setEnabled(true);

        userRepository.save(user);

        return "Administrador registrado correctamente";
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                     FORGOT PASSWORD
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    public String forgotPassword(ForgotPasswordRequest request) {

        passwordResetService.sendResetLink(request.getEmail());

        return "Se enviará un enlace de recuperación a su correo registrado";
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                     RESET PASSWORD
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    public String resetPassword(ResetPasswordRequest request) {

        passwordResetService.resetPassword(request);

        return "Contraseña actualizada correctamente";
    }

    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    //                     GOOGLE AUTH
    // . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    public AuthResponse googleAuth(GoogleAuthRequest request) {

        return googleAuthService.authenticate(
                request.getAccessToken(),
                request.isRememberMe()
        );
    }
}

