package com.sivemor.platform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sivemor.platform.domain.AuditLogRepository
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.security.JwtService
import com.sivemor.platform.service.AuditService
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.MySQLContainer

@Import(IntegrationTestSupport.NoopAuditConfig::class)
@SpringBootTest
abstract class IntegrationTestSupport {
    @Autowired
    lateinit var context: WebApplicationContext

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var jwtService: JwtService

    lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUpMockMvc() {
        SecurityContextHolder.clearContext()
        val builder = MockMvcBuilders.webAppContextSetup(context)
        builder.apply<DefaultMockMvcBuilder>(springSecurity())
        mockMvc = builder.build()
    }

    fun login(username: String, password: String): JsonNode {
        val requestContent = objectMapper.writeValueAsString(
            mapOf(
                "username" to username,
                "password" to password
            )
        )

        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = requestContent
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return objectMapper.readTree(result.response.contentAsString)
    }

    fun bearerToken(username: String, password: String): String =
        "Bearer ${login(username, password)["accessToken"].asText()}"

    fun bearerTokenForUser(username: String): String {
        val user = requireNotNull(userRepository.findByUsernameIgnoreCaseAndArchivedFalse(username)) {
            "User $username was not found in test data"
        }
        return "Bearer ${jwtService.generateAccessToken(user)}"
    }

    companion object {
        private val mysql = MySQLContainer("mysql:8.0.36")
            .withDatabaseName("sivemor_test")
            .withUsername("sivemor")
            .withPassword("sivemor")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            if (!mysql.isRunning) {
                mysql.start()
            }
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.flyway.enabled") { "false" }
            registry.add("app.security.jwt.secret") {
                "this-is-a-long-enough-secret-for-test-only-1234567890"
            }
        }
    }

    @TestConfiguration
    class NoopAuditConfig {
        @Bean
        @Primary
        fun auditService(): AuditService = AuditService(
            auditLogRepository = mockk<AuditLogRepository>(relaxed = true),
            objectMapper = ObjectMapper()
        )
    }
}
