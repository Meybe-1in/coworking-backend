package com.coworking.storage.service;

import com.coworking.config.SupabaseConfig;
import com.coworking.storage.exception.InvalidStorageFileException;
import com.coworking.storage.exception.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupabaseStorageServiceTest {

    @Mock
    private SupabaseConfig config;

    @Mock
    private HttpClient httpClient;

    private SupabaseStorageService storageService;

    @BeforeEach
    void setUp() {

        storageService = new SupabaseStorageService(config);

        ReflectionTestUtils.setField(
                storageService,
                "maxFileSize",
                DataSize.ofMegabytes(5)
        );

        ReflectionTestUtils.setField(
                storageService,
                "httpClient",
                httpClient
        );
    }

    private void mockSupabaseConfig() {

        when(config.getUrl())
                .thenReturn("https://test.supabase.co");

        when(config.getBucket())
                .thenReturn("rooms");

        when(config.getApiKey())
                .thenReturn("test-api-key");
    }

    @Test
    void upload_emptyFile_throwsException() {

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        assertThrows(
                InvalidStorageFileException.class,
                () -> storageService.upload(file)
        );

        verifyNoInteractions(httpClient);
    }

    @Test
    void upload_unsupportedFileType_throwsException() {

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "document.pdf",
                "application/pdf",
                "contenido".getBytes()
        );

        assertThrows(
                InvalidStorageFileException.class,
                () -> storageService.upload(file)
        );

        verifyNoInteractions(httpClient);
    }

    @Test
    void upload_fileExceedsMaxSize_throwsException() {

        byte[] content = new byte[
                (int) DataSize.ofMegabytes(5).toBytes() + 1
                ];

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "large.jpg",
                "image/jpeg",
                content
        );

        assertThrows(
                InvalidStorageFileException.class,
                () -> storageService.upload(file)
        );

        verifyNoInteractions(httpClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void upload_success_returnsPublicUrl() throws Exception {

        mockSupabaseConfig();

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "room.jpg",
                "image/jpeg",
                "contenido de prueba".getBytes()
        );

        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(200);

        doReturn(response)
                .when(httpClient)
                .send(
                        any(HttpRequest.class),
                        any(HttpResponse.BodyHandler.class)
                );

        String result = storageService.upload(file);

        assertNotNull(result);
        assertTrue(
                result.startsWith(
                        "https://test.supabase.co/storage/v1/object/public/rooms/"
                )
        );

        assertTrue(result.endsWith(".jpg"));

        verify(httpClient).send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void upload_supabaseError_throwsStorageException() throws Exception {

        mockSupabaseConfig();

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "room.png",
                "image/png",
                "contenido".getBytes()
        );

        HttpResponse<String> response = mock(HttpResponse.class);

        when(response.statusCode()).thenReturn(403);

        doReturn(response)
                .when(httpClient)
                .send(
                        any(HttpRequest.class),
                        any(HttpResponse.BodyHandler.class)
                );

        assertThrows(
                StorageException.class,
                () -> storageService.upload(file)
        );
    }
}
