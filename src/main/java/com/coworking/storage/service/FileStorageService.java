package com.coworking.storage.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService implements StorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("No se puede crear la carpeta de uploads", e);
        }
    }

    @Override
    public String upload(MultipartFile file) {
        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        // extensión (si existe)
        String ext = "";
        int i = original.lastIndexOf('.');
        if (i > 0) ext = original.substring(i);

        // Generar nombre único
        String filename = UUID.randomUUID().toString() + ext;

        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Archivo vacío");
            }
            if (original.contains("..")) {
                throw new RuntimeException("Nombre de archivo inválido: " + original);
            }

            Path target = this.rootLocation.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // Retornar la ruta relativa que guardaremos en DB (ej: /uploads/uuid.jpg)
            return "/uploads/" + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Fallo al almacenar archivo " + original, ex);
        }
    }
}
