package com.coworking.storage.service;

import com.coworking.config.SupabaseConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.coworking.storage.exception.StorageException;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import java.util.UUID;


@Service
@Primary
@RequiredArgsConstructor
public class SupabaseStorageService implements StorageService {


    private final SupabaseConfig config;


    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Override
    public String upload(MultipartFile file) {
        // Validar el archivo
        if (file == null || file.isEmpty()) {
            throw new StorageException("El archivo está vacío");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new StorageException(
                    "Tipo de archivo no permitido: "
                            + file.getContentType()
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new StorageException(
                    "La imagen supera el tamaño máximo permitido de 5 MB"
            );
        }

        // Subir el archivo a Supabase
        try {
            String original = file.getOriginalFilename();
            // Obtener la extensión del archivo
            String extension = "";

            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf("."));
            }

            String filename = UUID.randomUUID() + extension;

            String uploadUrl = config.getUrl()
                    + "/storage/v1/object/"
                    + config.getBucket()
                    + "/"
                    + filename;

            // Construir la solicitud PUT
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", file.getContentType())
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();


            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());


            if (response.statusCode() >= 400) {
                throw new StorageException(
                        "Error subiendo archivo a Supabase: "
                                + response.body()
                );
            }


            return config.getUrl()
                    + "/storage/v1/object/public/"
                    + config.getBucket()
                    + "/"
                    + filename;


        } catch (Exception e) {
            throw new StorageException(
                    "Error al subir imagen a Supabase", e
            );
        }
    }

}
