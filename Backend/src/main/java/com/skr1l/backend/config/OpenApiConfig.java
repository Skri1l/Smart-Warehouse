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

    private static final String BASIC_AUTH = "basicAuth";

    @Bean
    public OpenAPI smartWarehouseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Warehouse API")
                        .description("REST API for warehouse, shelf, stock, device and replenishment management")
                        .version("0.0.1")
                        .license(new License().name("See LICENSE")))
                .components(new Components()
                        .addSecuritySchemes(BASIC_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
    }
}
