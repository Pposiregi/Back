package com.fitpet.server.shared.config;

import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;

@Slf4j
@Configuration
public class FcmConfig {

    @Value("${fcm.service-account-key-path}")
    private String serviceAccountKeyPath;

    @Value("${fcm.project-id}")
    private String propertiesProjectId;

    private FirebaseApp firebaseApp;

    private final ResourceLoader resourceLoader;

    public FcmConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void initialize() {
        try {
//            ClassPathResource resource = new ClassPathResource(serviceAccountKeyPath);

            // 2025.12.24 KKR]  GCP 설정파일 읽기위함
            Resource resource = resourceLoader.getResource(serviceAccountKeyPath);

            if (!resource.exists()) {
                throw new RuntimeException("❌ FCM 키 파일을 찾을 수 없습니다. 경로를 확인해주세요: " + serviceAccountKeyPath);
            }

            try (InputStream serviceAccountStream = resource.getInputStream()) {
                GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);

                String realProjectId = null;
                if (credentials instanceof ServiceAccountCredentials) {
                    realProjectId = ((ServiceAccountCredentials) credentials).getProjectId();
                }

                log.info("=================================================================");
                log.info("🔥 FCM 설정 진단 시작");
                log.info("   1. 키 파일 경로: {}", serviceAccountKeyPath);
                log.info("   2. application.yml에 설정된 ID: {}", propertiesProjectId);
                log.info("   3. JSON 키 파일 내부의 실제 ID: {}", realProjectId);

                if (realProjectId != null && !realProjectId.equals(propertiesProjectId)) {
                    log.error("🚨 [경고] 설정 파일(yml)의 ID와 키 파일(json)의 ID가 다릅니다!");
                    log.error("🚨 JSON 파일의 ID({})가 우선 적용됩니다.", realProjectId);
                } else {
                    log.info("✅ 프로젝트 ID가 일치합니다.");
                }
                log.info("=================================================================");

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setProjectId(realProjectId != null ? realProjectId : propertiesProjectId)
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    this.firebaseApp = FirebaseApp.initializeApp(options);
                    log.info("🎉 FirebaseApp 초기화 성공 (Target Project ID: {})",
                            this.firebaseApp.getOptions().getProjectId());
                } else {
                    this.firebaseApp = FirebaseApp.getInstance();
                    log.info("⚠ FirebaseApp이 이미 존재합니다. (Existing Project ID: {})",
                            this.firebaseApp.getOptions().getProjectId());
                }
            }
        } catch (IOException e) {
            log.error("❌ FCM 초기화 실패: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (this.firebaseApp == null) {
            try {
                this.firebaseApp = FirebaseApp.getInstance();
            } catch (Exception e) {
                throw new IllegalStateException("FirebaseApp이 정상적으로 초기화되지 않았습니다.", e);
            }
        }
        return FirebaseMessaging.getInstance(this.firebaseApp);
    }
}