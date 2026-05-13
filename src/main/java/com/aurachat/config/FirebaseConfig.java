package com.aurachat.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for Firebase Admin SDK.
 * Initializes Firebase with service account credentials.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account-key-path}")
    private String serviceAccountKeyPath;

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount;

            // Prefer project-root path if present
            Path projectPath = Paths.get("src/main/resources/serviceAccountKey.json");
            if (Files.exists(projectPath)) {
                serviceAccount = new FileInputStream(projectPath.toFile());
                log.info("Loading Firebase service account key from project path: {}", projectPath);
            } else 
            
            // Try to load from classpath first (for resources in src/main/resources)
            if (serviceAccountKeyPath.startsWith("classpath:")) {
                String path = serviceAccountKeyPath.substring("classpath:".length());
                Resource resource = new ClassPathResource(path);
                serviceAccount = resource.getInputStream();
                log.info("Loading Firebase service account key from classpath: {}", path);
            } else if (serviceAccountKeyPath.startsWith("src/main/resources/")) {
                // Handle relative path to resources folder
                String resourcePath = serviceAccountKeyPath.substring("src/main/resources/".length());
                Resource resource = new ClassPathResource(resourcePath);
                serviceAccount = resource.getInputStream();
                log.info("Loading Firebase service account key from resources: {}", resourcePath);
            } else {
                // Load from absolute file path
                serviceAccount = new FileInputStream(serviceAccountKeyPath);
                log.info("Loading Firebase service account key from file: {}", serviceAccountKeyPath);
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully");
            } else {
                log.info("Firebase Admin SDK already initialized");
            }

        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }
}