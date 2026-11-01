package com.example.userapp.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Paths;

public class DataDirectoryInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (Exception ignored) {
        }
    }
}
