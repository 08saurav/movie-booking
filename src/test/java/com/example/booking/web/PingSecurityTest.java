package com.example.booking.web;

import com.example.booking.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Segment 1 acceptance criteria: the stack boots and role-based
 * access control behaves correctly end-to-end.
 */
class PingSecurityTest extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void anonymousRequestIsRejected() {
        ResponseEntity<String> response = rest.getForEntity("/api/ping", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void authenticatedCustomerCanPing() {
        ResponseEntity<String> response = rest
                .withBasicAuth("customer", "customer123")
                .getForEntity("/api/ping", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("pong");
    }

    @Test
    void customerCannotAccessAdminEndpoint() {
        ResponseEntity<String> response = rest
                .withBasicAuth("customer", "customer123")
                .getForEntity("/api/admin/ping", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanAccessAdminEndpoint() {
        ResponseEntity<String> response = rest
                .withBasicAuth("admin", "admin123")
                .getForEntity("/api/admin/ping", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("admin pong");
    }
}
