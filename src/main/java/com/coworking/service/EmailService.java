package com.coworking.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail (String to, String token){
        try{
        String link = "http://localhost:8080/auth/verify?token=" + token;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("Coworking Web <dc8848349@gmail.com>");
        helper.setTo(to);
        helper.setSubject("Verifica tu cuenta - Coworking Web");

        helper.setText("""
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
                """.formatted(link), true);

        mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Error enviando correo", e);
        }
    }
}
