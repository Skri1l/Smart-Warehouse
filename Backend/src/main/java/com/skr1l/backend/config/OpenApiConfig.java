package com.skr1l.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /** Name of the JWT scheme; public operations opt out with an empty {@code @SecurityRequirements}. */
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI smartWarehouseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Warehouse API")
                        .description("REST API for warehouse, shelf, stock, device and replenishment management")
                        .version("0.0.1")
                        .license(new License().name("See LICENSE")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token from POST /auth/login. Valid for 15 minutes.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
