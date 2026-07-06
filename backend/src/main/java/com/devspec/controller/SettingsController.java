package com.devspec.controller;

import com.devspec.model.User;
import com.devspec.repository.UserRepository;
import com.devspec.service.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final SystemSettingService settingService;
    private final UserRepository userRepository;

    public SettingsController(SystemSettingService settingService, UserRepository userRepository) {
        this.settingService = settingService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getSettings(Authentication authentication) {
        // Any authenticated user can view settings (e.g. to check active provider or upload limits)
        return ResponseEntity.ok(settingService.getAllSettings());
    }

    @PostMapping
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, String> requestSettings, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied: Only administrators can update system settings.");
        }

        logger.info("Admin '{}' is updating system settings", user.getUsername());
        for (Map.Entry<String, String> entry : requestSettings.entrySet()) {
            settingService.updateSetting(entry.getKey(), entry.getValue());
        }

        return ResponseEntity.ok(Map.of("message", "System settings updated successfully"));
    }
}
