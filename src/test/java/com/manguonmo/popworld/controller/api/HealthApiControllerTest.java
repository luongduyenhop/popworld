package com.manguonmo.popworld.controller.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthApiControllerTest {

    private final HealthApiController controller = new HealthApiController();

    @Test
    @DisplayName("Health Check: Endpoint /api/health trả về status UP và HTTP 200")
    void checkHealth_ShouldReturnUp() {
        ResponseEntity<Map<String, Object>> response = controller.checkHealth();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("PopWorld Art Toy Platform", response.getBody().get("service"));
        assertNotNull(response.getBody().get("timestamp"));
    }
}
