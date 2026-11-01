package com.fielddispatch.web;

import com.fielddispatch.domain.SystemSetting;
import com.fielddispatch.repo.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class DiagnosticsController {
    private final SystemSettingRepository systemSettingRepository;
    private final Environment environment;
    private final String datasourceUrl;
    private final String datasourceUsername;

    public DiagnosticsController(SystemSettingRepository systemSettingRepository,
                                 Environment environment,
                                 @Value("${spring.datasource.url}") String datasourceUrl,
                                 @Value("${spring.datasource.username}") String datasourceUsername) {
        this.systemSettingRepository = systemSettingRepository;
        this.environment = environment;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
    }

    @GetMapping("/diagnostics")
    public Map<String, Object> diagnostics() {
        Map<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("application", environment.getProperty("spring.application.name", "field-service-dispatch-system"));
        diagnostics.put("javaVersion", System.getProperty("java.version"));
        diagnostics.put("datasourceUrl", datasourceUrl);
        diagnostics.put("datasourceUsername", datasourceUsername);
        diagnostics.put("commandServerPort", environment.getProperty("app.command-server.port"));
        diagnostics.put("jwtIssuer", environment.getProperty("app.jwt.issuer"));

        Map<String, String> settings = new LinkedHashMap<>();
        for (SystemSetting setting : systemSettingRepository.findAll()) {
            settings.put(setting.getSettingKey(), setting.getSettingValue());
        }
        diagnostics.put("settings", settings);
        return diagnostics;
    }
}
