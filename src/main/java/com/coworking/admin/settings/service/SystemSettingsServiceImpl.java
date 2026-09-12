package com.coworking.admin.settings.service;

import com.coworking.admin.settings.dto.SystemSettingsResponse;
import com.coworking.admin.settings.dto.UpdateSystemSettingsRequest;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.repository.SystemSettingsRepository;
import com.coworking.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class SystemSettingsServiceImpl implements SystemSettingsService {

    private static final String DEFAULT_CONFIG_KEY = "GLOBAL";

    private static final LocalTime DEFAULT_OPENING_TIME =
            LocalTime.of(7, 0);

    private static final LocalTime DEFAULT_CLOSING_TIME =
            LocalTime.of(20, 0);

    private static final int DEFAULT_MAX_RESERVATION_HOURS = 8;

    private static final int DEFAULT_PENDING_EXPIRATION_MINUTES = 15;

    private final SystemSettingsRepository systemSettingsRepository;

    @Override
    public SystemSettingsResponse getSettings() {

        SystemSettings settings = getOrCreateSettings();

        return mapToResponse(settings);
    }

    @Override
    @Transactional
    public SystemSettingsResponse updateSettings(
            UpdateSystemSettingsRequest request
    ) {

        validateBusinessRules(request);

        SystemSettings settings = getOrCreateSettings();

        settings.setOpeningTime(request.openingTime());
        settings.setClosingTime(request.closingTime());
        settings.setMaxReservationHours(
                request.maxReservationHours()
        );
        settings.setPendingExpirationMinutes(
                request.pendingExpirationMinutes()
        );
        settings.setInstitutionName(
                request.institutionName()
        );
        settings.setInstitutionEmail(
                request.institutionEmail()
        );
        settings.setInstitutionPhone(
                request.institutionPhone()
        );
        settings.setInstitutionAddress(
                request.institutionAddress()
        );

        SystemSettings savedSettings =
                systemSettingsRepository.save(settings);

        return mapToResponse(savedSettings);
    }

    @Override
    @Transactional
    public SystemSettings getCurrentSettings() {
        return getOrCreateSettings();
    }

    private SystemSettings getOrCreateSettings() {

        return systemSettingsRepository
                .findByConfigKey(DEFAULT_CONFIG_KEY)
                .orElseGet(this::createDefaultSettings);
    }

    private SystemSettings createDefaultSettings() {

        SystemSettings settings = new SystemSettings();

        settings.setConfigKey(DEFAULT_CONFIG_KEY);
        settings.setOpeningTime(DEFAULT_OPENING_TIME);
        settings.setClosingTime(DEFAULT_CLOSING_TIME);
        settings.setMaxReservationHours(
                DEFAULT_MAX_RESERVATION_HOURS
        );
        settings.setPendingExpirationMinutes(
                DEFAULT_PENDING_EXPIRATION_MINUTES
        );

        settings.setInstitutionName("Coworking Platform");

        return systemSettingsRepository.save(settings);
    }

    private void validateBusinessRules(
            UpdateSystemSettingsRequest request
    ) {

        if (request == null) {
            throw new BadRequestException("La configuración es obligatoria");
        }

        if (request.openingTime() == null ||
                request.closingTime() == null) {

            throw new BadRequestException("El horario de atención es obligatorio");
        }

        if (request.maxReservationHours() == null ||
                request.maxReservationHours() < 1) {

            throw new BadRequestException("La duración máxima debe ser mayor que 0");
        }

        if (request.pendingExpirationMinutes() == null ||
                request.pendingExpirationMinutes() < 1) {

            throw new BadRequestException("El tiempo de expiración debe ser mayor que 0");
        }

        if (request.institutionName() == null ||
                request.institutionName().isBlank()) {

            throw new BadRequestException("El nombre institucional es obligatorio");
        }

        if (!request.openingTime().isBefore(
                request.closingTime()
        )) {

            throw new BadRequestException("La hora de apertura debe ser anterior a la hora de cierre");
        }

        long availableMinutes = Duration.between(
                request.openingTime(),
                request.closingTime()
        ).toMinutes();

        long maxReservationMinutes =
                request.maxReservationHours() * 60L;

        if (maxReservationMinutes > availableMinutes) {

            throw new BadRequestException("La duración máxima no puede superar el horario permitido");
        }
    }

    private SystemSettingsResponse mapToResponse(
            SystemSettings settings
    ) {

        return new SystemSettingsResponse(
                settings.getOpeningTime(),
                settings.getClosingTime(),
                settings.getMaxReservationHours(),
                settings.getPendingExpirationMinutes(),
                settings.getInstitutionName(),
                settings.getInstitutionEmail(),
                settings.getInstitutionPhone(),
                settings.getInstitutionAddress(),
                settings.getUpdatedAt()
        );
    }
}