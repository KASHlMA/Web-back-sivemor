package com.sivemor.platform.mobile

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.common.ForbiddenException
import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.ChecklistTemplateRepository
import com.sivemor.platform.domain.InspectionRepository
import com.sivemor.platform.domain.InspectionResult
import com.sivemor.platform.domain.InspectionStatus
import com.sivemor.platform.domain.OrderUnitRepository
import com.sivemor.platform.domain.Role
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.domain.Verificacion
import com.sivemor.platform.domain.VerificationOrderStatus
import com.sivemor.platform.service.AuditService
import com.sivemor.platform.service.MerCompatibilityService
import com.sivemor.platform.support.TestEntityFactory.checklistQuestion
import com.sivemor.platform.support.TestEntityFactory.checklistSection
import com.sivemor.platform.support.TestEntityFactory.inspection
import com.sivemor.platform.support.TestEntityFactory.inspectionAnswer
import com.sivemor.platform.support.TestEntityFactory.inspectionEvidence
import com.sivemor.platform.support.TestEntityFactory.orderUnit
import com.sivemor.platform.support.TestEntityFactory.templateWithSection
import com.sivemor.platform.support.TestEntityFactory.user
import com.sivemor.platform.support.TestEntityFactory.vehicle
import com.sivemor.platform.support.TestEntityFactory.verificationOrder
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class MobileInspectionServiceTest {
    @MockK private lateinit var orderUnitRepository: OrderUnitRepository
    @MockK private lateinit var checklistTemplateRepository: ChecklistTemplateRepository
    @MockK private lateinit var inspectionRepository: InspectionRepository
    @MockK private lateinit var userRepository: UserRepository
    @MockK private lateinit var auditService: AuditService
    @MockK private lateinit var merCompatibilityService: MerCompatibilityService

    private lateinit var mobileInspectionService: MobileInspectionService
    private val clock: Clock = Clock.fixed(Instant.parse("2026-03-23T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mobileInspectionService = MobileInspectionService(
            orderUnitRepository,
            checklistTemplateRepository,
            inspectionRepository,
            userRepository,
            auditService,
            merCompatibilityService,
            clock
        )
        every { auditService.log(any(), any(), any(), any(), any()) } just Runs
        every { merCompatibilityService.syncSubmittedInspection(any()) } returns Verificacion()
    }

    @Test
    fun `listAssignedOrders excludes units with submitted inspections`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val order = verificationOrder(id = 4L, technician = technician)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val submittedInspection = inspection(
            id = 7L,
            order = order,
            orderUnit = orderUnit,
            technician = technician,
            status = InspectionStatus.SUBMITTED
        )

        every { orderUnitRepository.findAllAssignedToTechnician(2L) } returns listOf(orderUnit)
        every { inspectionRepository.findByOrderUnitIdAndArchivedFalse(5L) } returns submittedInspection

        assertThat(mobileInspectionService.listAssignedOrders(2L)).isEmpty()
    }

    @Test
    fun `createInspection returns an existing draft instead of creating a duplicate`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val order = verificationOrder(id = 4L, technician = technician)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val existingDraft = inspection(
            id = 7L,
            order = order,
            orderUnit = orderUnit,
            technician = technician,
            template = templateWithSection()
        )

        every { orderUnitRepository.findByIdAndArchivedFalse(5L) } returns orderUnit
        every { inspectionRepository.findByOrderUnitIdAndArchivedFalse(5L) } returns existingDraft

        val result = mobileInspectionService.createInspection(2L, CreateInspectionRequest(orderUnitId = 5L))

        assertThat(result.id).isEqualTo(7L)
        verify(exactly = 0) { inspectionRepository.save(any()) }
    }

    @Test
    fun `createInspection rejects access from the wrong technician`() {
        val assignedTechnician = user(id = 2L, role = Role.TECHNICIAN)
        val order = verificationOrder(id = 4L, technician = assignedTechnician)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))

        every { orderUnitRepository.findByIdAndArchivedFalse(5L) } returns orderUnit
        every { inspectionRepository.findByOrderUnitIdAndArchivedFalse(5L) } returns null

        assertThatThrownBy {
            mobileInspectionService.createInspection(99L, CreateInspectionRequest(orderUnitId = 5L))
        }.isInstanceOf(ForbiddenException::class.java)
            .hasMessage("Inspection is not assigned to this technician")
    }

    @Test
    fun `updateDraftInspection rejects questions that are not part of the requested section`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val template = templateWithSection()
        val extraSection = checklistSection(id = 20L, template = template, questions = mutableListOf())
        val extraQuestion = checklistQuestion(id = 21L, section = extraSection, code = "EXTRA")
        extraSection.questions += extraQuestion
        template.sections += extraSection

        val order = verificationOrder(id = 4L, technician = technician)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val draft = inspection(id = 7L, order = order, orderUnit = orderUnit, technician = technician, template = template)

        every { inspectionRepository.findDetailedById(7L) } returns draft

        assertThatThrownBy {
            mobileInspectionService.updateDraftInspection(
                2L,
                7L,
                UpdateInspectionRequest(
                    sections = listOf(
                        SectionUpdateRequest(
                            sectionId = template.sections.first().id!!,
                            questions = listOf(QuestionUpdateRequest(questionId = 21L, answer = AnswerValue.PASS))
                        )
                    )
                )
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Question 21 does not belong to section 1")
    }

    @Test
    fun `addEvidence rejects empty uploads`() {
        val emptyFile = MockMultipartFile("file", ByteArray(0))

        assertThatThrownBy {
            mobileInspectionService.addEvidence(1L, 2L, emptyFile, 3L, null, null)
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Evidence file cannot be empty")
    }

    @Test
    fun `addEvidence stores checksum metadata and increments evidence count`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val template = templateWithSection()
        val section = template.sections.first()
        val order = verificationOrder(id = 4L, technician = technician)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val draft = inspection(id = 7L, order = order, orderUnit = orderUnit, technician = technician, template = template)
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", "hello".encodeToByteArray())

        every { inspectionRepository.findDetailedById(7L) } returns draft

        val response = mobileInspectionService.addEvidence(2L, 7L, file, section.id!!, "comment", null)

        assertThat(response.evidenceCount).isEqualTo(1)
        assertThat(draft.evidences.single().checksum).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824")
        verify { auditService.log(technician, "ADD_INSPECTION_EVIDENCE", "INSPECTION", "7", any()) }
    }

    @Test
    fun `pauseInspection marks the draft as paused`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val template = templateWithSection()
        val order = verificationOrder(id = 4L, technician = technician)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val draft = inspection(id = 7L, order = order, orderUnit = orderUnit, technician = technician, template = template)

        every { inspectionRepository.findDetailedById(7L) } returns draft

        val response = mobileInspectionService.pauseInspection(2L, 7L)

        assertThat(response.status).isEqualTo(InspectionStatus.PAUSED)
        assertThat(draft.status).isEqualTo(InspectionStatus.PAUSED)
        verify { auditService.log(technician, "PAUSE_INSPECTION", "INSPECTION", "7", any()) }
    }

    @Test
    fun `deleteEvidence removes evidence from the draft`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val template = templateWithSection()
        val section = template.sections.first()
        val order = verificationOrder(id = 4L, technician = technician)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val draft = inspection(id = 7L, order = order, orderUnit = orderUnit, technician = technician, template = template)
        draft.evidences += inspectionEvidence(id = 20L, inspection = draft, section = section)

        every { inspectionRepository.findDetailedById(7L) } returns draft

        val response = mobileInspectionService.deleteEvidence(2L, 7L, 20L)

        assertThat(response.evidenceCount).isZero()
        assertThat(draft.evidences).isEmpty()
        verify { auditService.log(technician, "REMOVE_INSPECTION_EVIDENCE", "INSPECTION", "7", any()) }
    }

    @Test
    fun `abandonInspection archives the draft and reopens the order when no active inspections remain`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val template = templateWithSection()
        val order = verificationOrder(id = 4L, technician = technician, status = VerificationOrderStatus.IN_PROGRESS)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val draft = inspection(id = 7L, order = order, orderUnit = orderUnit, technician = technician, template = template)

        every { inspectionRepository.findDetailedById(7L) } returns draft
        every { orderUnitRepository.countByVerificationOrderIdAndArchivedFalse(4L) } returns 1L
        every { inspectionRepository.countByVerificationOrderIdAndStatusAndArchivedFalse(4L, InspectionStatus.SUBMITTED) } returns 0L
        every { inspectionRepository.countByVerificationOrderIdAndArchivedFalse(4L) } returns 0L

        mobileInspectionService.abandonInspection(2L, 7L)

        assertThat(draft.archived).isTrue()
        assertThat(order.status).isEqualTo(VerificationOrderStatus.OPEN)
        verify { auditService.log(technician, "ABANDON_INSPECTION", "INSPECTION", "7", any()) }
    }

    @Test
    fun `submitInspection requires all required answers and at least three evidences`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val template = templateWithSection()
        val order = verificationOrder(id = 4L, technician = technician)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val draft = inspection(id = 7L, order = order, orderUnit = orderUnit, technician = technician, template = template)

        every { inspectionRepository.findDetailedById(7L) } returns draft

        assertThatThrownBy {
            mobileInspectionService.submitInspection(2L, 7L)
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Required questions are still unanswered")

        template.sections.first().questions.forEachIndexed { index, question ->
            draft.answers += inspectionAnswer(
                id = (10 + index).toLong(),
                inspection = draft,
                question = question,
                answerValue = AnswerValue.PASS
            )
        }

        assertThatThrownBy {
            mobileInspectionService.submitInspection(2L, 7L)
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("At least three evidences are required before submission")
    }

    @Test
    fun `submitInspection computes overall result and completes the order when all units are submitted`() {
        val technician = user(id = 2L, role = Role.TECHNICIAN)
        val template = templateWithSection()
        val order = verificationOrder(id = 4L, technician = technician, status = VerificationOrderStatus.IN_PROGRESS)
        val orderUnit = orderUnit(id = 5L, order = order, vehicle = vehicle(id = 6L, client = order.clientCompany))
        val draft = inspection(id = 7L, order = order, orderUnit = orderUnit, technician = technician, template = template)
        val section = template.sections.first()

        section.questions.forEachIndexed { index, question ->
            draft.answers += inspectionAnswer(
                id = (10 + index).toLong(),
                inspection = draft,
                question = question,
                answerValue = if (index == 0) AnswerValue.FAIL else AnswerValue.PASS
            )
        }
        draft.evidences += inspectionEvidence(id = 20L, inspection = draft, section = section)
        draft.evidences += inspectionEvidence(id = 21L, inspection = draft, section = section)
        draft.evidences += inspectionEvidence(id = 22L, inspection = draft, section = section)

        every { inspectionRepository.findDetailedById(7L) } returns draft
        every { orderUnitRepository.countByVerificationOrderIdAndArchivedFalse(4L) } returns 1L
        every { inspectionRepository.countByVerificationOrderIdAndStatusAndArchivedFalse(4L, InspectionStatus.SUBMITTED) } returns 1L

        val response = mobileInspectionService.submitInspection(2L, 7L)

        assertThat(response.status).isEqualTo(InspectionStatus.SUBMITTED)
        assertThat(draft.overallResult).isEqualTo(InspectionResult.FAIL)
        assertThat(order.status).isEqualTo(VerificationOrderStatus.COMPLETED)
        assertThat(draft.submittedAt).isEqualTo(Instant.now(clock))
        verify { merCompatibilityService.syncSubmittedInspection(draft) }
    }
}
