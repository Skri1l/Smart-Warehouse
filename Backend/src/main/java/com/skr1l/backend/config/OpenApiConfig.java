package com.skr1l.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartWarehouseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Warehouse API")
                        .description("REST API for warehouse, shelf, stock, device and replenishment management")
                        .version("0.0.1")
                        .license(new License().name("See LICENSE")));
    }
}
