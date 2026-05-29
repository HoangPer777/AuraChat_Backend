package com.aurachat.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration for Firebase Admin SDK.
 * Initializes Firebase with service account credentials.
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        String path = "serviceAccountKey.json";
        java.io.File file = new java.io.File(path);
        
        if (!file.exists()) {
            log.warn("Firebase service account key file NOT FOUND at: {}. Firebase features will be disabled.", path);
            return;
        }
        
        if (file.isDirectory()) {
            log.warn("Firebase service account key path '{}' is a DIRECTORY, not a file. Firebase features will be disabled.", path);
            return;
        }

        try (InputStream serviceAccount = new FileInputStream(file)) {
            log.info("Loading Firebase service account key from file: {}", path);

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
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
            // We don't throw exception here to allow the app to start without Firebase
        }
    }
}
