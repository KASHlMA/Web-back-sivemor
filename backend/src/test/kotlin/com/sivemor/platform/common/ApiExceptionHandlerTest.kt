package com.sivemor.platform.common

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

class ApiExceptionHandlerTest {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
        mockMvc = MockMvcBuilders.standaloneSetup(TestController())
            .setControllerAdvice(ApiExceptionHandler())
            .setValidator(validator)
            .build()
    }

    @Test
    fun `maps application exceptions to the declared status`() {
        mockMvc.get("/api-error").andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Bad Request") }
            jsonPath("$.message") { value("Nope") }
            jsonPath("$.path") { value("/api-error") }
        }
    }

    @Test
    fun `maps validation errors into field details`() {
        val result = mockMvc.post("/validation") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"value":""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Validation failed") }
            jsonPath("$.details.value") { exists() }
        }.andReturn()

        assertThat(result.response.contentAsString).contains("Validation failed")
    }

    @Test
    fun `maps access denied and unexpected exceptions`() {
        mockMvc.get("/denied").andExpect {
            status { isForbidden() }
            jsonPath("$.message") { value("Forbidden area") }
        }

        mockMvc.get("/boom").andExpect {
            status { isInternalServerError() }
            jsonPath("$.message") { value("kaboom") }
        }
    }

    @RestController
    private class TestController {
        @GetMapping("/api-error")
        fun apiError(): String = throw BadRequestException("Nope")

        @PostMapping("/validation")
        fun validation(@Valid @RequestBody request: ValidationBody): String = request.value

        @GetMapping("/denied")
        fun denied(): String = throw AccessDeniedException("Forbidden area")

        @GetMapping("/boom")
        fun boom(): String = throw IllegalStateException("kaboom")
    }

    private data class ValidationBody(
        @field:NotBlank val value: String
    )
}
