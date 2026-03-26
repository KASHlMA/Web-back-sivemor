package com.sivemor.platform.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties(
    var issuer: String = "sivemor-platform",
    var secret: String = "replace-me-with-a-64-char-secret-in-env",
    var accessTokenMinutes: Long = 60,
    var refreshTokenDays: Long = 14
)

@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    var allowedOrigins: List<String> = listOf("http://localhost:3000")
)
