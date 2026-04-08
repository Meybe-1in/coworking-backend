package com.coworking.email.service;

import com.coworking.domain.notification.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.hibernate.mapping.Map;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailService implements EmailSender {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void send(String to, String subject, String template, java.util.Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);

        String html = templateEngine.process(template, context);

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
