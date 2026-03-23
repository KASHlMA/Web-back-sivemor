package com.sivemor.platform.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("SIVEMOR Platform API")
                .version(currentVersion())
                .description("Generated API documentation for the SIVEMOR platform.")
        )
        .components(
            Components().addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )
        )

    private fun currentVersion(): String =
        OpenApiConfig::class.java.`package`.implementationVersion ?: "0.1.0-SNAPSHOT"
}
