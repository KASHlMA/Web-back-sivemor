package com.sivemor.platform

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class AuthApiIntegrationTest : IntegrationTestSupport() {
    @Test
    fun `refresh rotates the session and logout revokes the new refresh token`() {
        val loginResponse = login("admin", "Admin123!")
        val refreshPayload = objectMapper.writeValueAsString(
            mapOf("refreshToken" to loginResponse["refreshToken"].asText())
        )

        val refreshResult = mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = refreshPayload
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { exists() }
        }.andReturn()

        val rotated = objectMapper.readTree(refreshResult.response.contentAsString)
        assertThat(rotated["refreshToken"].asText()).isNotEqualTo(loginResponse["refreshToken"].asText())

        val logoutPayload = objectMapper.writeValueAsString(
            mapOf("refreshToken" to rotated["refreshToken"].asText())
        )

        mockMvc.post("/api/v1/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = logoutPayload
        }.andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
        }

        mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = logoutPayload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Refresh token is invalid") }
        }
    }

    @Test
    fun `invalid refresh tokens are rejected`() {
        mockMvc.post("/api/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"does-not-exist"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Refresh token is invalid") }
        }
    }
}
