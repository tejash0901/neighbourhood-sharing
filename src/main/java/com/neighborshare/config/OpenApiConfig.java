package com.neighborshare.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String bearerAuth = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("NeighborhoodShare API")
                .description("Backend API for neighborhood item sharing, bookings, payments, reviews, and disputes")
                .version("0.1.0"))
            .addSecurityItem(new SecurityRequirement().addList(bearerAuth))
            .components(new Components().addSecuritySchemes(
                bearerAuth,
                new SecurityScheme()
                    .name(bearerAuth)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            ));
    }
}
