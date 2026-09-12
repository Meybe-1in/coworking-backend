package com.coworking.admin.settings.controller;

import com.coworking.admin.settings.dto.SystemSettingsResponse;
import com.coworking.admin.settings.dto.UpdateSystemSettingsRequest;
import com.coworking.admin.settings.service.SystemSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    @GetMapping
    public ResponseEntity<SystemSettingsResponse> getSettings() {

        return ResponseEntity.ok(
                systemSettingsService.getSettings()
        );
    }

    @PutMapping
    public ResponseEntity<SystemSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateSystemSettingsRequest request
    ) {

        return ResponseEntity.ok(
                systemSettingsService.updateSettings(request)
        );
    }
}