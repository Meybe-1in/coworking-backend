package com.coworking.service.settingsAdmin;

import com.coworking.admin.settings.dto.SystemSettingsResponse;
import com.coworking.admin.settings.dto.UpdateSystemSettingsRequest;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.repository.SystemSettingsRepository;
import com.coworking.admin.settings.service.SystemSettingsServiceImpl;
import com.coworking.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SystemSettingsServiceImplTest {

    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    @InjectMocks
    private SystemSettingsServiceImpl systemSettingsService;

    private SystemSettings settings;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        settings = new SystemSettings();

        settings.setId(1L);
        settings.setConfigKey("GLOBAL");
        settings.setOpeningTime(LocalTime.of(7, 0));
        settings.setClosingTime(LocalTime.of(20, 0));
        settings.setMaxReservationHours(8);
        settings.setPendingExpirationMinutes(15);
        settings.setInstitutionName("Coworking Platform");
        settings.setInstitutionEmail("admin@coworking.com");
        settings.setInstitutionPhone("2222-2222");
        settings.setInstitutionAddress("San Miguel, El Salvador");
        settings.setUpdatedAt(
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }

    @Test
    void getSettings_shouldReturnExistingSettings() {

        // Given
        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.of(settings));

        // When
        SystemSettingsResponse response =
                systemSettingsService.getSettings();

        // Then
        assertNotNull(response);

        assertEquals(
                LocalTime.of(7, 0),
                response.openingTime()
        );

        assertEquals(
                LocalTime.of(20, 0),
                response.closingTime()
        );

        assertEquals(
                8,
                response.maxReservationHours()
        );

        assertEquals(
                15,
                response.pendingExpirationMinutes()
        );

        assertEquals(
                "Coworking Platform",
                response.institutionName()
        );

        assertEquals(
                "admin@coworking.com",
                response.institutionEmail()
        );

        assertEquals(
                "2222-2222",
                response.institutionPhone()
        );

        assertEquals(
                "San Miguel, El Salvador",
                response.institutionAddress()
        );

        assertEquals(
                settings.getUpdatedAt(),
                response.updatedAt()
        );

        verify(systemSettingsRepository)
                .findByConfigKey("GLOBAL");
    }

    @Test
    void getSettings_shouldCreateDefaultSettingsWhenNoneExist() {

        // Given
        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.empty());

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SystemSettingsResponse response =
                systemSettingsService.getSettings();

        // Then
        assertNotNull(response);

        assertEquals(
                LocalTime.of(7, 0),
                response.openingTime()
        );

        assertEquals(
                LocalTime.of(20, 0),
                response.closingTime()
        );

        assertEquals(
                8,
                response.maxReservationHours()
        );

        assertEquals(
                15,
                response.pendingExpirationMinutes()
        );

        assertEquals(
                "Coworking Platform",
                response.institutionName()
        );

        verify(systemSettingsRepository)
                .save(any(SystemSettings.class));
    }

    @Test
    void getCurrentSettings_shouldReturnExistingSettings() {

        // Given
        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.of(settings));

        // When
        SystemSettings result =
                systemSettingsService.getCurrentSettings();

        // Then
        assertNotNull(result);
        assertEquals(settings, result);

        verify(systemSettingsRepository)
                .findByConfigKey("GLOBAL");
    }

    @Test
    void getCurrentSettings_shouldCreateDefaultSettingsWhenNoneExist() {

        // Given
        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.empty());

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SystemSettings result =
                systemSettingsService.getCurrentSettings();

        // Then
        assertNotNull(result);

        assertEquals(
                LocalTime.of(7, 0),
                result.getOpeningTime()
        );

        assertEquals(
                LocalTime.of(20, 0),
                result.getClosingTime()
        );

        assertEquals(
                8,
                result.getMaxReservationHours()
        );

        assertEquals(
                15,
                result.getPendingExpirationMinutes()
        );

        assertEquals(
                "Coworking Platform",
                result.getInstitutionName()
        );

        verify(systemSettingsRepository)
                .save(any(SystemSettings.class));
    }

    @Test
    void updateSettings_shouldUpdateAllFields() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0),
                        6,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.of(settings));

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SystemSettingsResponse response =
                systemSettingsService.updateSettings(request);

        // Then
        assertNotNull(response);

        assertEquals(
                LocalTime.of(8, 0),
                response.openingTime()
        );

        assertEquals(
                LocalTime.of(18, 0),
                response.closingTime()
        );

        assertEquals(
                6,
                response.maxReservationHours()
        );

        assertEquals(
                30,
                response.pendingExpirationMinutes()
        );

        assertEquals(
                "Nueva Institución",
                response.institutionName()
        );

        assertEquals(
                "contacto@nueva.com",
                response.institutionEmail()
        );

        assertEquals(
                "7777-7777",
                response.institutionPhone()
        );

        assertEquals(
                "Nueva dirección",
                response.institutionAddress()
        );

        verify(systemSettingsRepository)
                .save(settings);
    }

    @Test
    void updateSettings_shouldCreateSettingsWhenNoneExist() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0),
                        6,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.empty());

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SystemSettingsResponse response =
                systemSettingsService.updateSettings(request);

        // Then
        assertNotNull(response);

        assertEquals(
                LocalTime.of(8, 0),
                response.openingTime()
        );

        assertEquals(
                LocalTime.of(18, 0),
                response.closingTime()
        );

        assertEquals(
                6,
                response.maxReservationHours()
        );

        assertEquals(
                30,
                response.pendingExpirationMinutes()
        );

        assertEquals(
                "Nueva Institución",
                response.institutionName()
        );

        verify(systemSettingsRepository, times(2))
                .save(any(SystemSettings.class));
    }

    @Test
    void updateSettings_shouldRejectOpeningTimeAfterClosingTime() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(18, 0),
                        LocalTime.of(8, 0),
                        6,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        // When + Then
        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(request)
        );

        verify(
                systemSettingsRepository,
                never()
        ).save(any(SystemSettings.class));
    }

    @Test
    void updateSettings_shouldRejectEqualOpeningAndClosingTime() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 0),
                        6,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        // When + Then
        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(request)
        );

        verify(
                systemSettingsRepository,
                never()
        ).save(any(SystemSettings.class));
    }

    @Test
    void updateSettings_shouldRejectMaxReservationGreaterThanAvailableHours() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        5,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        // 08:00 - 12:00 = 4 horas
        // maximum = 5 horas

        // When + Then
        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(request)
        );

        verify(
                systemSettingsRepository,
                never()
        ).save(any(SystemSettings.class));
    }

    @Test
    void updateSettings_shouldAllowMaxReservationEqualToAvailableHours() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        4,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.of(settings));

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SystemSettingsResponse response =
                systemSettingsService.updateSettings(request);

        // Then
        assertNotNull(response);

        assertEquals(
                4,
                response.maxReservationHours()
        );

        verify(systemSettingsRepository)
                .save(settings);
    }

    @Test
    void updateSettings_shouldValidateUsingMinutes() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 30),
                        5,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.of(settings));

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 08:00 - 12:30 = 4.5 horas
        // maximum = 5 horas
        // Debe rechazar porque 5h > 4h30m

        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(request)
        );

        verify(
                systemSettingsRepository,
                never()
        ).save(any(SystemSettings.class));
    }

    @Test
    void updateSettings_shouldPreserveInstitutionOptionalFieldsWhenProvided() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0),
                        6,
                        30,
                        "Coworking San Miguel",
                        null,
                        null,
                        null
                );

        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.of(settings));

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SystemSettingsResponse response =
                systemSettingsService.updateSettings(request);

        // Then
        assertNotNull(response);

        assertEquals(
                "Coworking San Miguel",
                response.institutionName()
        );

        assertNull(response.institutionEmail());
        assertNull(response.institutionPhone());
        assertNull(response.institutionAddress());

        verify(systemSettingsRepository)
                .save(settings);
    }

    @Test
    void updateSettings_shouldSaveUpdatedEntity() {

        // Given
        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(9, 0),
                        LocalTime.of(19, 0),
                        5,
                        45,
                        "Coworking Updated",
                        "updated@coworking.com",
                        "8888-8888",
                        "Updated address"
                );

        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.of(settings));

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        systemSettingsService.updateSettings(request);

        // Then
        ArgumentCaptor<SystemSettings> captor =
                ArgumentCaptor.forClass(SystemSettings.class);

        verify(systemSettingsRepository)
                .save(captor.capture());

        SystemSettings saved =
                captor.getValue();

        assertEquals(
                LocalTime.of(9, 0),
                saved.getOpeningTime()
        );

        assertEquals(
                LocalTime.of(19, 0),
                saved.getClosingTime()
        );

        assertEquals(
                5,
                saved.getMaxReservationHours()
        );

        assertEquals(
                45,
                saved.getPendingExpirationMinutes()
        );

        assertEquals(
                "Coworking Updated",
                saved.getInstitutionName()
        );

        assertEquals(
                "updated@coworking.com",
                saved.getInstitutionEmail()
        );

        assertEquals(
                "8888-8888",
                saved.getInstitutionPhone()
        );

        assertEquals(
                "Updated address",
                saved.getInstitutionAddress()
        );
    }

    @Test
    void getSettings_shouldCreateDefaultSettingsWithGlobalConfigKey() {

        when(systemSettingsRepository.findByConfigKey("GLOBAL"))
                .thenReturn(Optional.empty());

        when(systemSettingsRepository.save(any(SystemSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SystemSettingsResponse response =
                systemSettingsService.getSettings();

        assertNotNull(response);

        ArgumentCaptor<SystemSettings> captor =
                ArgumentCaptor.forClass(SystemSettings.class);

        verify(systemSettingsRepository)
                .save(captor.capture());

        SystemSettings savedSettings =
                captor.getValue();

        assertEquals("GLOBAL", savedSettings.getConfigKey());
    }

    @Test
    void updateSettings_shouldRejectNullRequest() {

        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(null)
        );

        verify(systemSettingsRepository, never())
                .save(any(SystemSettings.class));
    }

    @Test
    void updateSettings_shouldRejectNullOpeningTime() {

        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        null,
                        LocalTime.of(18, 0),
                        6,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(request)
        );

        verify(systemSettingsRepository, never())
                .save(any(SystemSettings.class));
    }
    @Test
    void updateSettings_shouldRejectNullClosingTime() {

        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        null,
                        6,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(request)
        );

        verify(systemSettingsRepository, never())
                .save(any(SystemSettings.class));
    }
    @Test
    void updateSettings_shouldRejectNullMaxReservationHours() {

        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0),
                        null,
                        30,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(request)
        );

        verify(systemSettingsRepository, never())
                .save(any(SystemSettings.class));
    }
    @Test
    void updateSettings_shouldRejectNullPendingExpirationMinutes() {

        UpdateSystemSettingsRequest request =
                new UpdateSystemSettingsRequest(
                        LocalTime.of(8, 0),
                        LocalTime.of(18, 0),
                        6,
                        null,
                        "Nueva Institución",
                        "contacto@nueva.com",
                        "7777-7777",
                        "Nueva dirección"
                );

        assertThrows(
                BadRequestException.class,
                () -> systemSettingsService.updateSettings(request)
        );

        verify(systemSettingsRepository, never())
                .save(any(SystemSettings.class));
    }
}
