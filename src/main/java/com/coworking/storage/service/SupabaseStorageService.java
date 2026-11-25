package com.coworking.storage.service;

import com.coworking.config.SupabaseConfig;
import com.coworking.storage.exception.InvalidStorageFileException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import com.coworking.storage.exception.StorageException;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

@Service
@Primary
@RequiredArgsConstructor
public class SupabaseStorageService implements StorageService {

    private final SupabaseConfig config;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.storage.max-file-size:5MB}")
    private DataSize maxFileSize;

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    @Override
    public String upload(MultipartFile file) {

        validateFile(file);

        String contentType = file.getContentType();
        String extension = ALLOWED_TYPES.get(contentType);

        String filename = UUID.randomUUID() + "." + extension;

        try {

            String baseUrl = config.getUrl().replaceAll("/+$", "");

            String uploadUrl = baseUrl
                    + "/storage/v1/object/"
                    + config.getBucket()
                    + "/"
                    + filename;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header(
                            "Authorization",
                            "Bearer " + config.getApiKey()
                    )
                    .header("Content-Type", contentType)
                    .PUT(
                            HttpRequest.BodyPublishers.ofByteArray(
                                    file.getBytes()
                            )
                    )
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                throw new StorageException(
                        "Supabase rechazó la subida de la imagen. "
                                + "Código HTTP: " + response.statusCode()
                );
            }

            return baseUrl
                    + "/storage/v1/object/public/"
                    + config.getBucket()
                    + "/"
                    + filename;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new StorageException(
                    "La subida de la imagen fue interrumpida",
                    e
            );

        } catch (IOException e) {

            throw new StorageException(
                    "No fue posible enviar la imagen a Supabase",
                    e
            );
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidStorageFileException(
                    "Debes seleccionar una imagen"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !ALLOWED_TYPES.containsKey(contentType)) {

            throw new InvalidStorageFileException(
                    "Formato de imagen no permitido. "
                            + "Solo se acepta JPG, PNG o WEBP"
            );
        }

        if (file.getSize() > maxFileSize.toBytes()) {

            throw new InvalidStorageFileException(
                    "La imagen supera el tamaño máximo permitido de "
                            + maxFileSize.toMegabytes()
                            + " MB"
            );
        }
    }
}