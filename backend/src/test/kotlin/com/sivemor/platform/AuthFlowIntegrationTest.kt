package com.sivemor.platform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper
) {
    @Test
    fun `admin can log in and access admin endpoints`() {
        val response = login("admin", "Admin123!")

        mockMvc.get("/api/v1/admin/users") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${response["accessToken"].asText()}")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].username") { exists() }
        }
    }

    @Test
    fun `technician cannot access admin endpoints`() {
        val response = login("tecnico1", "Tecnico123!")

        mockMvc.get("/api/v1/admin/users") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${response["accessToken"].asText()}")
        }.andExpect {
            status { isForbidden() }
        }
    }

    private fun login(username: String, password: String): JsonNode {
        val content = objectMapper.writeValueAsString(
            mapOf(
                "username" to username,
                "password" to password
            )
        )

        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = content
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return objectMapper.readTree(result.response.contentAsString)
    }

    companion object {
        @Container
        private val mysql = MySQLContainer("mysql:8.4")
            .withDatabaseName("sivemor_test")
            .withUsername("sivemor")
            .withPassword("sivemor")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("app.security.jwt.secret") {
                "this-is-a-long-enough-secret-for-test-only-1234567890"
            }
        }
    }
}
