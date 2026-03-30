package com.coworking.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String token) {

        String link = "http://localhost:8080/auth/verify?token=" + token;

        String html = """
        <div style="font-family: Arial; padding:20px">
          <h2>Bienvenido a Coworking Web</h2>
          <p>Gracias por registrarte.</p>
          <p>Haz clic en el botón para activar tu cuenta:</p>

          <a href="%s"
             style="display:inline-block;
                    padding:12px 20px;
                    background:#4f46e5;
                    color:white;
                    text-decoration:none;
                    border-radius:6px;">
             Verificar cuenta
          </a>

          <p style="margin-top:20px;font-size:12px;color:#666">
             Este enlace expira en 24 horas.
          </p>
        </div>
        """.formatted(link);

        sendHtmlEmail(to, "Verifica tu cuenta - Coworking Web", html);
    }

    public void sendPasswordResetEmail(String email, String token) {
        String link = "http://localhost:5173/reset-password?token=" + token;

        String html = """
        <h2>Restablecer contraseña</h2>
        <p>Haz clic en el siguiente enlace para cambiar tu contraseña:</p>
        <a href="%s">Restablecer contraseña</a>
        <p>Este enlace expira en 1 hora.</p>
        """.formatted(link);

        sendHtmlEmail(email, "Recuperar contraseña", html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("Coworking Web <dc8848349@gmail.com>");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando correo", e);
        }
    }
}
