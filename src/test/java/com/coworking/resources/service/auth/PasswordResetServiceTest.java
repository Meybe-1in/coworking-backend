package com.coworking.resources.service.auth;

import com.coworking.auth.service.PasswordResetService;
import com.coworking.domain.notification.EmailSender;
import com.coworking.auth.model.PasswordResetToken;
import com.coworking.email.template.EmailTemplate;
import com.coworking.user.model.User;
import com.coworking.auth.repository.PasswordResetTokenRepository;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailSender emailSender;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Test
    void shouldSendResetEmail() {

        User user = new User();
        user.setEmail("test@mail.com");

        when(userRepository.findByEmail("test@mail.com"))
                .thenReturn(Optional.of(user));

        passwordResetService.sendResetLink("test@mail.com");

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailSender).send(
                eq("test@mail.com"),
                eq("Recuperar contraseña"),
                eq(EmailTemplate.RESET_PASSWORD.getPath()),
                anyMap()
        );
    }
}
