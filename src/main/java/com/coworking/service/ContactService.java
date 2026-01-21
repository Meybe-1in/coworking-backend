package com.coworking.service;

import com.coworking.dto.ContactRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ContactService {

    private final JavaMailSender mailSender;


    public void sendContactEmail(@Valid ContactRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("CoworkingWeb <relaxation.comes@gmail.com>");
        message.setTo("dc8848349@gmail.com");
        message.setSubject("Contactanos Coworking Web");
        message.setText(
                "Nombre: " + request.getName() + "\nEmail: " + request.getEmail() + "\nMensaje:\n" + request.getMessage()

        );

        mailSender.send(message);
    }
}
