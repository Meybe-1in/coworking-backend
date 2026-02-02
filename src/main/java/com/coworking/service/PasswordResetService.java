package com.coworking.service;

import com.coworking.dto.ResetPasswordRequest;
import com.coworking.model.PasswordResetToken;
import com.coworking.model.User;
import com.coworking.repository.PasswordResetTokenRepository;
import com.coworking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // resend link
    public void sendResetLink(String email) throws BadRequestException {
        userRepository.findByEmail(email).ifPresent(user -> {
            // eliminar tokens previos
            tokenRepository.deleteByUser(user);

            PasswordResetToken token = PasswordResetToken.create(user);
            tokenRepository.save(token);

            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    token.getToken()
            );
        });
    }

    public void resetPassword(ResetPasswordRequest request) throws BadRequestException {
        if (!request.getNewPassword().equals(request.getConfirmPassword())){
            throw new BadRequestException("Las contraseñas no coinciden");
        }

        PasswordResetToken token = tokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() ->
                        new BadRequestException("Token inválido"));

        if (token.isExpired()){
            throw new BadRequestException("El token ha expirado");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        tokenRepository.deleteByUser(user);
    }
}