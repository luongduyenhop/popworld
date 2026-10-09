package com.manguonmo.popworld.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint kiểm tra tình trạng sức khỏe của ứng dụng (Healthcheck Probe).
 * Phục vụ cho:
 * - Docker HEALTHCHECK chỉ thị container sẵn sàng
 * - Cloudflare Tunnel & Load Balancer kiểm tra uptime
 * - Giám sát tự động (Uptime monitoring tools)
 */
@RestController
@RequestMapping("/api/health")
public class HealthApiController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "service", "PopWorld Art Toy Platform",
                "version", "0.0.1-SNAPSHOT"
        ));
    }
}
