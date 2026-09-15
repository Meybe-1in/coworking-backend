package com.coworking.admin.settings.controller;


import com.coworking.admin.settings.dto.PublicReservationSettingsResponse;
import com.coworking.admin.settings.entity.SystemSettings;
import com.coworking.admin.settings.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class PublicSettingsController {

    private final SystemSettingsService systemSettingsService;

    @GetMapping("/reservation")
    public ResponseEntity<PublicReservationSettingsResponse> getReservationSettings() {

        SystemSettings settings =
                systemSettingsService.getCurrentSettings();

        return ResponseEntity.ok(
                new PublicReservationSettingsResponse(
                        settings.getOpeningTime(),
                        settings.getClosingTime(),
                        settings.getMaxReservationHours(),
                        settings.getPendingExpirationMinutes()
                )
        );
    }
}
