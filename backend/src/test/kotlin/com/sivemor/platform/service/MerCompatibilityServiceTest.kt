package com.sivemor.platform.service

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.Evaluacion
import com.sivemor.platform.domain.EvaluacionRepository
import com.sivemor.platform.domain.Verificacion
import com.sivemor.platform.domain.VerificacionRepository
import com.sivemor.platform.domain.VerificacionVeredicto
import com.sivemor.platform.support.TestEntityFactory.checklistQuestion
import com.sivemor.platform.support.TestEntityFactory.checklistSection
import com.sivemor.platform.support.TestEntityFactory.checklistTemplate
import com.sivemor.platform.support.TestEntityFactory.inspection
import com.sivemor.platform.support.TestEntityFactory.inspectionAnswer
import com.sivemor.platform.support.TestEntityFactory.inspectionEvidence
import com.sivemor.platform.support.TestEntityFactory.inspectionNote
import com.sivemor.platform.support.TestEntityFactory.orderUnit
import com.sivemor.platform.support.TestEntityFactory.user
import com.sivemor.platform.support.TestEntityFactory.vehicle
import com.sivemor.platform.support.TestEntityFactory.verificationOrder
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MerCompatibilityServiceTest {
    @MockK private lateinit var verificacionRepository: VerificacionRepository
    @MockK private lateinit var evaluacionRepository: EvaluacionRepository

    private lateinit var service: MerCompatibilityService
    private val clock: Clock = Clock.fixed(Instant.parse("2026-04-14T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        service = MerCompatibilityService(verificacionRepository, evaluacionRepository, clock)
    }

    @Test
    fun `syncSubmittedInspection creates verificacion and evaluacion from mapped answers`() {
        val technician = user(id = 2L)
        val order = verificationOrder(id = 10L, technician = technician)
        val orderUnit = orderUnit(id = 11L, order = order, vehicle = vehicle(id = 12L, client = order.clientCompany))
        val template = checklistTemplate(sections = mutableListOf())
        val luces = checklistSection(id = 101L, template = template, title = "Luces", questions = mutableListOf())
        val llantas = checklistSection(id = 102L, template = template, title = "Llantas", questions = mutableListOf())
        template.sections += mutableListOf(luces, llantas)
        val lucesQuestion = checklistQuestion(id = 201L, section = luces, code = "luces_galibo")
        val pressureQuestion = checklistQuestion(id = 202L, section = llantas, code = "llantas_presion_delantera_izquierda")
        luces.questions += lucesQuestion
        llantas.questions += pressureQuestion

        val draft = inspection(id = 30L, order = order, orderUnit = orderUnit, technician = technician, template = template).apply {
            submittedAt = Instant.parse("2026-04-14T10:00:00Z")
            overallComment = "Todo revisado"
        }
        draft.answers += inspectionAnswer(id = 301L, inspection = draft, question = lucesQuestion, answerValue = AnswerValue.FAIL)
        draft.answers += inspectionAnswer(
            id = 302L,
            inspection = draft,
            question = pressureQuestion,
            answerValue = AnswerValue.PASS,
            comment = "95"
        )
        draft.sectionNotes += inspectionNote(id = 401L, inspection = draft, section = luces, comment = "Falla en galibo")
        draft.evidences += inspectionEvidence(id = 501L, inspection = draft, section = luces)
        draft.evidences += inspectionEvidence(id = 502L, inspection = draft, section = luces)
        draft.evidences += inspectionEvidence(id = 503L, inspection = draft, section = luces)

        every { verificacionRepository.findByInspectionIdAndArchivedFalse(30L) } returns null
        every { verificacionRepository.save(any()) } answers {
            firstArg<Verificacion>().apply { id = 600L }
        }
        every { evaluacionRepository.findByVerificacionIdAndArchivedFalse(600L) } returns null
        every { evaluacionRepository.save(any()) } answers { firstArg<Evaluacion>().apply { id = 601L } }

        val result = service.syncSubmittedInspection(draft)

        assertThat(result.id).isEqualTo(600L)
        assertThat(result.veredicto).isEqualTo(VerificacionVeredicto.REPROBADO)

        val evaluacionSlot = slot<Evaluacion>()
        verify { evaluacionRepository.save(capture(evaluacionSlot)) }
        assertThat(evaluacionSlot.captured.lucesGalibo).isEqualTo("REPROBADO")
        assertThat(evaluacionSlot.captured.llantasPresionDelanteraIzquierda).isEqualTo(95.0)
        assertThat(evaluacionSlot.captured.comentarioLuces).isEqualTo("Falla en galibo")
        assertThat(evaluacionSlot.captured.comentariosGenerales).isEqualTo("Todo revisado")
        assertThat(evaluacionSlot.captured.evidenceCount).isEqualTo(3)
    }

    @Test
    fun `syncSubmittedInspection rejects invalid numeric answer comments`() {
        val technician = user(id = 2L)
        val order = verificationOrder(id = 10L, technician = technician)
        val orderUnit = orderUnit(id = 11L, order = order, vehicle = vehicle(id = 12L, client = order.clientCompany))
        val template = checklistTemplate(sections = mutableListOf())
        val llantas = checklistSection(id = 102L, template = template, title = "Llantas", questions = mutableListOf())
        template.sections += llantas
        val pressureQuestion = checklistQuestion(id = 202L, section = llantas, code = "llantas_presion_delantera_izquierda")
        llantas.questions += pressureQuestion

        val draft = inspection(id = 30L, order = order, orderUnit = orderUnit, technician = technician, template = template)
        draft.answers += inspectionAnswer(
            id = 302L,
            inspection = draft,
            question = pressureQuestion,
            answerValue = AnswerValue.PASS,
            comment = "noventa"
        )

        every { verificacionRepository.findByInspectionIdAndArchivedFalse(30L) } returns null
        every { verificacionRepository.save(any()) } answers {
            firstArg<Verificacion>().apply { id = 600L }
        }
        every { evaluacionRepository.findByVerificacionIdAndArchivedFalse(600L) } returns null

        assertThatThrownBy { service.syncSubmittedInspection(draft) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessage("Invalid numeric value for llantas_presion_delantera_izquierda")
    }
}
