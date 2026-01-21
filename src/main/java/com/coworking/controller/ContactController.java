package com.coworking.controller;

import com.coworking.dto.ContactRequest;
import com.coworking.service.ContactService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/contact")
@Tag(name = "Contact", description = "Endpoint para formulario de contact")
@RequiredArgsConstructor
public class ContactController {
    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<?> SendContact(@Valid @RequestBody ContactRequest request){
        contactService.sendContactEmail(request);
        return ResponseEntity.ok(
                Map.of("message", "Mensaje enviado correctamente")
        );
    }

}
