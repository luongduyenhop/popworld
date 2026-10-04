package com.manguonmo.popworld;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class PopworldApplication {

    private static final Logger log = LoggerFactory.getLogger(PopworldApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(PopworldApplication.class, args);
    }

    private static void loadDotEnv() {
        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            try {
                List<String> lines = Files.readAllLines(envPath);
                int loadedCount = 0;
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        int idx = trimmed.indexOf('=');
                        String key = trimmed.substring(0, idx).trim();
                        String value = trimmed.substring(idx + 1).trim();

                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }

                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, value);
                            loadedCount++;
                        }
                    }
                }
                log.info("PopWorld: Đã tự động nạp {} biến cấu hình từ file .env", loadedCount);
            } catch (IOException e) {
                log.warn("PopWorld: Không thể đọc file .env: {}", e.getMessage());
            }
        }
    }
}
