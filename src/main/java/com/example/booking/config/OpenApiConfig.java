package com.example.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers an HTTP Basic security scheme on the OpenAPI document so the Swagger
 * UI shows an "Authorize" button. Enter the demo credentials there once and every
 * "Try it out" call is sent with the right Authorization header.
 */
@Configuration
public class OpenApiConfig {

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI movieBookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Movie Ticket Booking API")
                        .description("REST API for a movie ticket booking system.")
                        .version("v0.1"))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")));
    }
}
