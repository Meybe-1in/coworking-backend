package com.coworking.admin.settings.service;


import com.coworking.admin.settings.dto.SystemSettingsResponse;
import com.coworking.admin.settings.dto.UpdateSystemSettingsRequest;
import com.coworking.admin.settings.entity.SystemSettings;
import org.springframework.stereotype.Service;

@Service
public interface SystemSettingsService {

    SystemSettingsResponse getSettings();

    SystemSettingsResponse updateSettings(
            UpdateSystemSettingsRequest request
    );

    SystemSettings getCurrentSettings();
}
