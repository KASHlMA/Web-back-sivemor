package com.sivemor.platform.service

import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.Verificacion
import com.sivemor.platform.domain.VerificacionRepository
import com.sivemor.platform.domain.VerificacionVeredicto
import com.sivemor.platform.support.TestEntityFactory.checklistQuestion
import com.sivemor.platform.support.TestEntityFactory.checklistSection
import com.sivemor.platform.support.TestEntityFactory.checklistTemplate
import com.sivemor.platform.support.TestEntityFactory.inspection
import com.sivemor.platform.support.TestEntityFactory.inspectionAnswer
import com.sivemor.platform.support.TestEntityFactory.orderUnit
import com.sivemor.platform.support.TestEntityFactory.user
import com.sivemor.platform.support.TestEntityFactory.vehicle
import com.sivemor.platform.support.TestEntityFactory.verificationOrder
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MerCompatibilityServiceTest {
    @MockK private lateinit var verificacionRepository: VerificacionRepository

    private lateinit var service: MerCompatibilityService
    private val clock: Clock = Clock.fixed(Instant.parse("2026-04-14T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service = MerCompatibilityService(verificacionRepository, clock)
    }

    @Test
    fun `syncSubmittedInspection creates verificacion without precreating evaluacion`() {
        val technician = user(id = 2L)
        val order = verificationOrder(id = 10L, technician = technician)
        val orderUnit = orderUnit(id = 11L, order = order, vehicle = vehicle(id = 12L, client = order.clientCompany))
        val template = checklistTemplate(sections = mutableListOf())
        val luces = checklistSection(id = 101L, template = template, title = "Luces", questions = mutableListOf())
        template.sections += luces
        val lucesQuestion = checklistQuestion(id = 201L, section = luces, code = "luces_galibo")
        luces.questions += lucesQuestion

        val draft = inspection(id = 30L, order = order, orderUnit = orderUnit, technician = technician, template = template).apply {
            submittedAt = Instant.parse("2026-04-14T10:00:00Z")
            overallComment = "Todo revisado"
        }
        draft.answers += inspectionAnswer(id = 301L, inspection = draft, question = lucesQuestion, answerValue = AnswerValue.FAIL)

        every { verificacionRepository.findByInspectionIdAndArchivedFalse(30L) } returns null
        every { verificacionRepository.save(any()) } answers {
            firstArg<Verificacion>().apply { id = 600L }
        }

        val result = service.syncSubmittedInspection(draft)

        assertThat(result.id).isEqualTo(600L)
        assertThat(result.veredicto).isEqualTo(VerificacionVeredicto.REPROBADO)
        assertThat(result.overallComment).isEqualTo("Todo revisado")
    }
}
