package com.logshield.logshieldv2;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for LogShield v2 Swagger documentation.
 * Provides metadata shown at the top of the Swagger UI page.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI logShieldOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LogShield v2 API")
                        .description(
                                "Real-Time Log Anomaly Detector REST API. " +
                                        "Implements Cycle Sort, Binary Search, " +
                                        "MinHeap, and Sliding Window algorithms.")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Virochan V")
                                .url("https://github.com/virochan-v")));
    }
}