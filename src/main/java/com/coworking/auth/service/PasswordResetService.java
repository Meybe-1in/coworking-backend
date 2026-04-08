package com.coworking.auth.service;

import com.coworking.auth.dto.ResetPasswordRequest;
import com.coworking.domain.notification.EmailSender;
import com.coworking.auth.model.PasswordResetToken;
import com.coworking.email.template.EmailTemplate;
import com.coworking.exception.BadRequestException;
import com.coworking.user.model.User;
import com.coworking.auth.repository.PasswordResetTokenRepository;
import com.coworking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    // resend link
    public void sendResetLink(String email) throws BadRequestException {
        userRepository.findByEmail(email).ifPresent(user -> {
            // eliminar tokens previos
            tokenRepository.deleteByUser(user);

            PasswordResetToken token = PasswordResetToken.create(user);
            tokenRepository.save(token);

            String link = "http://localhost:5173/reset-password?token=" + token.getToken();

            emailSender.send(
                    user.getEmail(),
                    "Recuperar contraseña",
                    EmailTemplate.RESET_PASSWORD.getPath(),
                    Map.of("link", link)
            );
        });
    }

    public void resetPassword(ResetPasswordRequest request) throws BadRequestException {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Las contraseñas no coinciden");
        }

        PasswordResetToken token = tokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() ->
                        new BadRequestException("Token inválido"));

        if (token.isExpired()) {
            throw new BadRequestException("El token ha expirado");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenRepository.deleteByUser(user);
    }
}