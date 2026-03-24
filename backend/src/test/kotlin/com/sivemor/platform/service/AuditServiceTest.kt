package com.sivemor.platform.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sivemor.platform.domain.AuditLog
import com.sivemor.platform.domain.AuditLogRepository
import com.sivemor.platform.support.TestEntityFactory.user
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuditServiceTest {
    @MockK
    private lateinit var auditLogRepository: AuditLogRepository

    private lateinit var auditService: AuditService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        auditService = AuditService(auditLogRepository, ObjectMapper())
    }

    @Test
    fun `log serializes details into json`() {
        val captured = slot<AuditLog>()
        every { auditLogRepository.save(capture(captured)) } answers { captured.captured }

        auditService.log(
            actor = user(id = 1L),
            action = "CREATE_USER",
            entityName = "USER",
            entityId = "1",
            details = mapOf("username" to "admin")
        )

        assertThat(captured.captured.detailsJson).isEqualTo("""{"username":"admin"}""")
        assertThat(captured.captured.entityName).isEqualTo("USER")
    }

    @Test
    fun `log keeps detailsJson null when no details are supplied`() {
        val captured = slot<AuditLog>()
        every { auditLogRepository.save(capture(captured)) } answers { captured.captured }

        auditService.log(
            actor = null,
            action = "PING",
            entityName = "SYSTEM",
            entityId = "health"
        )

        assertThat(captured.captured.detailsJson).isNull()
    }
}
