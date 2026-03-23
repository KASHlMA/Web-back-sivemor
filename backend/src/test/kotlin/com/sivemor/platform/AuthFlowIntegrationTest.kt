package com.sivemor.platform

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
class AuthFlowIntegrationTest(
    @Autowired private val context: WebApplicationContext,
    @Autowired private val objectMapper: ObjectMapper
) {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .build()
    }

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
    fun `swagger endpoints are publicly available and advertise bearer auth`() {
        val apiDocsResult = mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.paths['/api/v1/auth/login']") { exists() }
            jsonPath("$.paths['/api/v1/admin/users']") { exists() }
            jsonPath("$.paths['/api/v1/mobile/orders']") { exists() }
            jsonPath("$.components.securitySchemes.bearerAuth.type") { value("http") }
            jsonPath("$.components.securitySchemes.bearerAuth.scheme") { value("bearer") }
        }.andReturn()

        val apiDocs = objectMapper.readTree(apiDocsResult.response.contentAsString)
        assertThat(apiDocs["components"]["securitySchemes"]["bearerAuth"]["bearerFormat"].asText()).isEqualTo("JWT")

        mockMvc.get("/v3/api-docs.yaml").andExpect {
            status { isOk() }
        }

        mockMvc.get("/swagger-ui/index.html").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `anonymous users cannot access admin endpoints`() {
        mockMvc.get("/api/v1/admin/users").andExpect {
            status { isForbidden() }
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

    @Test
    fun `technician can fetch current checklist`() {
        val response = login("tecnico1", "Tecnico123!")

        mockMvc.get("/api/v1/mobile/checklists/current") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${response["accessToken"].asText()}")
        }.andExpect {
            status { isOk() }
            jsonPath("$.sections[0].questions[0].id") { exists() }
        }
    }

    @Test
    fun `technician can create update and fetch inspection draft`() {
        val response = login("tecnico1", "Tecnico123!")
        val authHeader = "Bearer ${response["accessToken"].asText()}"

        val ordersResult = mockMvc.get("/api/v1/mobile/orders") {
            header(HttpHeaders.AUTHORIZATION, authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].orderUnitId") { exists() }
        }.andReturn()

        val orders = objectMapper.readTree(ordersResult.response.contentAsString)
        val orderUnitId = orders[0]["orderUnitId"].asLong()
        val createPayload = objectMapper.writeValueAsString(mapOf("orderUnitId" to orderUnitId))

        val createResult = mockMvc.post("/api/v1/mobile/inspections") {
            header(HttpHeaders.AUTHORIZATION, authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = createPayload
        }.andExpect {
            status { isCreated() }
            jsonPath("$.sections[0].questions[0].id") { exists() }
        }.andReturn()

        val inspection = objectMapper.readTree(createResult.response.contentAsString)
        val inspectionId = inspection["id"].asLong()
        val sections = inspection["sections"].map { section ->
            val questions = section["questions"].mapIndexed { index, question ->
                mapOf(
                    "questionId" to question["id"].asLong(),
                    "answer" to if (index == section["questions"].size() - 1) "FAIL" else "PASS",
                    "comment" to "Validated ${question["code"].asText()}"
                )
            }

            mapOf(
                "sectionId" to section["sectionId"].asLong(),
                "note" to "Checked ${section["title"].asText()}",
                "questions" to questions
            )
        }

        val updatePayload = objectMapper.writeValueAsString(
            mapOf(
                "lastSectionIndex" to (inspection["sections"].size() - 1),
                "overallComment" to "Integration validation",
                "sections" to sections
            )
        )

        mockMvc.put("/api/v1/mobile/inspections/$inspectionId") {
            header(HttpHeaders.AUTHORIZATION, authHeader)
            contentType = MediaType.APPLICATION_JSON
            content = updatePayload
        }.andExpect {
            status { isOk() }
            jsonPath("$.overallComment") { value("Integration validation") }
        }

        val readResult = mockMvc.get("/api/v1/mobile/inspections/$inspectionId") {
            header(HttpHeaders.AUTHORIZATION, authHeader)
        }.andExpect {
            status { isOk() }
            jsonPath("$.overallComment") { value("Integration validation") }
        }.andReturn()

        val updatedInspection = objectMapper.readTree(readResult.response.contentAsString)
        val totalQuestions = updatedInspection["sections"].sumOf { it["questions"].size() }
        val totalAnswers = updatedInspection["sections"].sumOf { it["answers"].size() }
        val sectionsWithNotes = updatedInspection["sections"].count { !it["note"].isNull }

        assertThat(totalAnswers).isEqualTo(totalQuestions)
        assertThat(sectionsWithNotes).isEqualTo(updatedInspection["sections"].size())
    }

    @Test
    fun `admin can view dashboard failures summary`() {
        val response = login("admin", "Admin123!")

        mockMvc.get("/api/v1/admin/dashboard/failures") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${response["accessToken"].asText()}")
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalSubmitted") { exists() }
            jsonPath("$.recentFailures") { isArray() }
        }
    }

    private fun login(username: String, password: String): JsonNode {
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
