package com.sivemor.platform.mobile

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.common.ForbiddenException
import com.sivemor.platform.common.NotFoundException
import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.ChecklistQuestion
import com.sivemor.platform.domain.ChecklistSection
import com.sivemor.platform.domain.ChecklistTemplate
import com.sivemor.platform.domain.ChecklistTemplateRepository
import com.sivemor.platform.domain.Inspection
import com.sivemor.platform.domain.InspectionAnswer
import com.sivemor.platform.domain.InspectionEvidence
import com.sivemor.platform.domain.InspectionRepository
import com.sivemor.platform.domain.InspectionResult
import com.sivemor.platform.domain.InspectionSectionNote
import com.sivemor.platform.domain.InspectionStatus
import com.sivemor.platform.domain.OrderUnitRepository
import com.sivemor.platform.domain.ClientCompanyRepository
import com.sivemor.platform.domain.RegionRepository
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.domain.VehicleCategory
import com.sivemor.platform.domain.VehicleUnit
import com.sivemor.platform.domain.VehicleUnitRepository
import com.sivemor.platform.domain.VerificationOrderRepository
import com.sivemor.platform.domain.VerificationOrderStatus
import com.sivemor.platform.security.AppUserPrincipal
import com.sivemor.platform.service.AuditService
import com.sivemor.platform.service.MerCompatibilityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant

data class AssignedOrderResponse(
    val orderUnitId: Long,
    val orderId: Long,
    val orderNumber: String,
    val clientCompanyId: Long,
    val clientCompanyName: String,
    val regionName: String,
    val scheduledAt: Instant,
    val vehicleUnitId: Long,
    val vehiclePlate: String,
    val vehicleCategory: String,
    val draftInspectionId: Long?
)

data class MobileClientResponse(
    val id: Long,
    val name: String,
    val regionId: Long?
)

data class MobileRegionResponse(
    val id: Long,
    val name: String
)

data class CreateMobileVehicleRequest(
    @field:NotNull val clientCompanyId: Long,
    val verificationOrderId: Long? = null,
    @field:NotBlank @field:Size(max = 20) val plate: String,
    @field:NotBlank @field:Size(max = 30) val vin: String,
    @field:NotNull val category: VehicleCategory = VehicleCategory.N2,
    @field:NotBlank @field:Size(max = 80) val brand: String,
    @field:NotBlank @field:Size(max = 80) val model: String = "Sin modelo"
)

data class MobileVehicleResponse(
    val id: Long,
    val clientCompanyId: Long,
    val clientCompanyName: String,
    val plate: String,
    val vin: String,
    val category: VehicleCategory,
    val brand: String,
    val model: String
)

data class ChecklistTemplateResponse(
    val id: Long,
    val name: String,
    val version: Int,
    val sections: List<ChecklistSectionResponse>
)

data class ChecklistSectionResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val displayOrder: Int,
    val questions: List<ChecklistQuestionResponse>
)

data class ChecklistQuestionResponse(
    val id: Long,
    val code: String,
    val prompt: String,
    val required: Boolean,
    val displayOrder: Int
)

data class CreateInspectionRequest(
    val orderUnitId: Long? = null,
    val vehicleUnitId: Long? = null
)

data class QuestionUpdateRequest(
    @field:NotNull val questionId: Long,
    @field:NotNull val answer: AnswerValue,
    val comment: String? = null
)

data class SectionUpdateRequest(
    @field:NotNull val sectionId: Long,
    val note: String? = null,
    val questions: List<QuestionUpdateRequest> = emptyList()
)

data class UpdateInspectionRequest(
    @field:Min(0) val lastSectionIndex: Int = 0,
    val overallComment: String? = null,
    @field:Valid val sections: List<SectionUpdateRequest> = emptyList()
)

data class InspectionQuestionAnswerResponse(
    val questionId: Long,
    val answer: AnswerValue,
    val comment: String?
)

data class EvidenceResponse(
    val id: Long,
    val sectionId: Long?,
    val filename: String,
    val mimeType: String,
    val capturedAt: Instant,
    val comment: String?
)

data class InspectionSectionDraftResponse(
    val sectionId: Long,
    val title: String,
    val description: String?,
    val displayOrder: Int,
    val note: String?,
    val questions: List<ChecklistQuestionResponse>,
    val answers: List<InspectionQuestionAnswerResponse>,
    val evidences: List<EvidenceResponse>
)

data class InspectionDraftResponse(
    val id: Long,
    val orderId: Long,
    val orderUnitId: Long,
    val orderNumber: String,
    val vehiclePlate: String,
    val clientCompanyName: String,
    val status: InspectionStatus,
    val lastSectionIndex: Int,
    val overallComment: String?,
    val startedAt: Instant,
    val updatedAt: Instant,
    val evidenceCount: Int,
    val sections: List<InspectionSectionDraftResponse>
)

data class SubmissionResponse(
    val inspectionId: Long,
    val status: InspectionStatus,
    val submittedAt: Instant?
)

@RestController
@Tag(name = "Mobile", description = "Technician mobile inspection endpoints")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/mobile")
class MobileInspectionController(
    private val mobileInspectionService: MobileInspectionService
) {
    @Operation(summary = "List order units assigned to the authenticated technician")
    @GetMapping("/orders")
    fun orders(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal
    ): List<AssignedOrderResponse> =
        mobileInspectionService.listAssignedOrders(principal.id)

    @Operation(summary = "List clients available for mobile vehicle registration")
    @GetMapping("/clients")
    fun clients(): List<MobileClientResponse> = mobileInspectionService.listClients()

    @Operation(summary = "List CEDIS available for mobile vehicle registration")
    @GetMapping("/regions")
    fun regions(): List<MobileRegionResponse> = mobileInspectionService.listRegions()

    @Operation(summary = "Create a vehicle unit from the mobile app")
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVehicle(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: CreateMobileVehicleRequest
    ): MobileVehicleResponse = mobileInspectionService.createVehicle(principal.id, request)

    @Operation(summary = "Load a vehicle unit from the mobile app")
    @GetMapping("/vehicles/{id}")
    fun getVehicle(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): MobileVehicleResponse = mobileInspectionService.getVehicle(principal.id, id)

    @Operation(summary = "Update a vehicle unit from the mobile app")
    @PutMapping("/vehicles/{id}")
    fun updateVehicle(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: CreateMobileVehicleRequest
    ): MobileVehicleResponse = mobileInspectionService.updateVehicle(principal.id, id, request)

    @Operation(summary = "Return the active checklist template with sections and questions")
    @GetMapping("/checklists/current")
    fun currentChecklist(): ChecklistTemplateResponse = mobileInspectionService.getCurrentChecklist()

    @Operation(summary = "Create a technician draft inspection for an assigned order unit or registered vehicle")
    @PostMapping("/inspections")
    @ResponseStatus(HttpStatus.CREATED)
    fun createInspection(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: CreateInspectionRequest
    ): InspectionDraftResponse = mobileInspectionService.createInspection(principal.id, request)

    @Operation(summary = "Load a technician draft inspection")
    @GetMapping("/inspections/{id}")
    fun getInspection(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): InspectionDraftResponse = mobileInspectionService.getDraftInspection(principal.id, id)

    @Operation(summary = "Update answers, notes, and progress for a technician draft inspection")
    @PutMapping("/inspections/{id}")
    fun updateInspection(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateInspectionRequest
    ): InspectionDraftResponse = mobileInspectionService.updateDraftInspection(principal.id, id, request)

    @Operation(summary = "Upload section evidence for a draft inspection")
    @PostMapping(
        "/inspections/{id}/evidences",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun addEvidence(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Parameter(
            description = "Evidence file to attach to the inspection draft",
            required = true,
            content = [Content(schema = Schema(type = "string", format = "binary"))]
        )
        @RequestPart("file") file: MultipartFile,
        @Parameter(description = "Checklist section receiving the evidence", required = true)
        @RequestParam sectionId: Long,
        @Parameter(description = "Optional technician comment for the uploaded evidence")
        @RequestParam(required = false) comment: String?,
        @Parameter(description = "Optional ISO-8601 timestamp indicating when the evidence was captured")
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        capturedAt: Instant?
    ): InspectionDraftResponse = mobileInspectionService.addEvidence(
        technicianId = principal.id,
        inspectionId = id,
        file = file,
        sectionId = sectionId,
        comment = comment,
        capturedAt = capturedAt
    )

    @Operation(summary = "Remove a previously uploaded evidence from a draft inspection")
    @DeleteMapping("/inspections/{id}/evidences/{evidenceId}")
    fun deleteEvidence(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @PathVariable evidenceId: Long
    ): InspectionDraftResponse = mobileInspectionService.deleteEvidence(principal.id, id, evidenceId)

    @Operation(summary = "Pause an in-progress draft inspection")
    @PostMapping("/inspections/{id}/pause")
    fun pauseInspection(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): InspectionDraftResponse = mobileInspectionService.pauseInspection(principal.id, id)

    @Operation(summary = "Abandon a draft inspection and archive it")
    @DeleteMapping("/inspections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun abandonInspection(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ) {
        mobileInspectionService.abandonInspection(principal.id, id)
    }

    @Operation(summary = "Submit a completed inspection draft")
    @PostMapping("/inspections/{id}/submit")
    fun submitInspection(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): SubmissionResponse = mobileInspectionService.submitInspection(principal.id, id)
}

@org.springframework.stereotype.Service
class MobileInspectionService(
    private val orderUnitRepository: OrderUnitRepository,
    private val vehicleUnitRepository: VehicleUnitRepository,
    private val clientCompanyRepository: ClientCompanyRepository,
    private val regionRepository: RegionRepository,
    private val verificationOrderRepository: VerificationOrderRepository,
    private val checklistTemplateRepository: ChecklistTemplateRepository,
    private val inspectionRepository: InspectionRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
    private val merCompatibilityService: MerCompatibilityService,
    private val clock: Clock
) {
    @Transactional(readOnly = true)
    fun listAssignedOrders(technicianId: Long): List<AssignedOrderResponse> = orderUnitRepository
        .findAllAssignedToTechnician(technicianId)
        .mapNotNull { orderUnit ->
            val existingInspection = inspectionRepository.findByOrderUnitIdAndArchivedFalse(orderUnit.id ?: 0L)
            if (existingInspection?.status == InspectionStatus.SUBMITTED) {
                null
            } else {
                AssignedOrderResponse(
                    orderUnitId = orderUnit.id ?: 0L,
                    orderId = orderUnit.verificationOrder.id ?: 0L,
                    orderNumber = orderUnit.verificationOrder.orderNumber,
                    clientCompanyId = orderUnit.verificationOrder.clientCompany.id ?: 0L,
                    clientCompanyName = orderUnit.verificationOrder.clientCompany.name,
                    regionName = orderUnit.verificationOrder.region.name,
                    scheduledAt = orderUnit.verificationOrder.scheduledAt,
                    vehicleUnitId = orderUnit.vehicleUnit.id ?: 0L,
                    vehiclePlate = orderUnit.vehicleUnit.plate,
                    vehicleCategory = orderUnit.vehicleUnit.category.name,
                    draftInspectionId = existingInspection?.id
                )
            }
        }

    @Transactional(readOnly = true)
    fun listClients(): List<MobileClientResponse> = clientCompanyRepository.findAllByArchivedFalseOrderByNameAsc()
        .map {
            MobileClientResponse(
                id = it.id ?: 0L,
                name = it.name,
                regionId = it.region?.id
            )
        }

    @Transactional(readOnly = true)
    fun listRegions(): List<MobileRegionResponse> = regionRepository.findAllByArchivedFalseOrderByNameAsc()
        .map {
            MobileRegionResponse(
                id = it.id ?: 0L,
                name = it.name
            )
        }

    @Transactional
    fun createVehicle(technicianId: Long, request: CreateMobileVehicleRequest): MobileVehicleResponse {
        val technician = requireTechnician(technicianId)
        val normalizedPlate = request.plate.trim().uppercase()
        val normalizedVin = request.vin.trim().uppercase()
        validateVehicleUniqueness(normalizedPlate, normalizedVin)
        val clientCompany = requireClientCompany(request.clientCompanyId)
        val verificationOrder = requireMobileVerificationOrder(technicianId, request.verificationOrderId, clientCompany.id ?: 0L)

        val vehicle = vehicleUnitRepository.save(
            VehicleUnit().apply {
                this.clientCompany = clientCompany
                plate = normalizedPlate
                vin = normalizedVin
                category = request.category
                brand = request.brand.trim()
                model = request.model.trim()
            }
        )

        verificationOrder?.let { order ->
            orderUnitRepository.save(
                com.sivemor.platform.domain.OrderUnit().apply {
                    this.verificationOrder = order
                    this.vehicleUnit = vehicle
                }
            )
            order.status = VerificationOrderStatus.IN_PROGRESS
        }

        return vehicle.also { savedVehicle ->
            auditService.log(
                actor = technician,
                action = "CREATE_MOBILE_VEHICLE",
                entityName = "VEHICLE_UNIT",
                entityId = savedVehicle.id.toString(),
                details = mapOf(
                    "plate" to savedVehicle.plate,
                    "verificationOrderId" to verificationOrder?.id
                )
            )
        }.toMobileVehicleResponse()
    }

    @Transactional(readOnly = true)
    fun getVehicle(technicianId: Long, vehicleId: Long): MobileVehicleResponse =
        requireMobileVehicle(technicianId, vehicleId).toMobileVehicleResponse()

    @Transactional
    fun updateVehicle(technicianId: Long, vehicleId: Long, request: CreateMobileVehicleRequest): MobileVehicleResponse {
        val technician = requireTechnician(technicianId)
        val normalizedPlate = request.plate.trim().uppercase()
        val normalizedVin = request.vin.trim().uppercase()
        validateVehicleUniqueness(normalizedPlate, normalizedVin, currentVehicleId = vehicleId)

        val vehicle = requireMobileVehicle(technicianId, vehicleId).apply {
            clientCompany = requireClientCompany(request.clientCompanyId)
            plate = normalizedPlate
            vin = normalizedVin
            category = request.category
            brand = request.brand.trim()
            model = request.model.trim()
        }

        auditService.log(
            actor = technician,
            action = "UPDATE_MOBILE_VEHICLE",
            entityName = "VEHICLE_UNIT",
            entityId = vehicle.id.toString(),
            details = mapOf(
                "plate" to vehicle.plate,
                "verificationOrderId" to request.verificationOrderId
            )
        )

        return vehicle.toMobileVehicleResponse()
    }

    @Transactional(readOnly = true)
    fun getCurrentChecklist(): ChecklistTemplateResponse = requireCurrentTemplate().toChecklistResponse()

    @Transactional
    fun createInspection(technicianId: Long, request: CreateInspectionRequest): InspectionDraftResponse {
        if (request.orderUnitId == null && request.vehicleUnitId == null) {
            throw BadRequestException("Either orderUnitId or vehicleUnitId must be provided")
        }

        val technician = requireTechnician(technicianId)
        val orderUnit = when {
            request.orderUnitId != null -> {
                val unit = orderUnitRepository.findByIdAndArchivedFalse(request.orderUnitId)
                    ?: throw NotFoundException("Order unit ${request.orderUnitId} was not found")

                if (unit.verificationOrder.assignedTechnician.id != technicianId) {
                    throw ForbiddenException("Inspection is not assigned to this technician")
                }
                unit
            }

            else -> requireOrCreateMobileOrderUnit(technician, request.vehicleUnitId!!)
        }

        val existingInspection = inspectionRepository.findByOrderUnitIdAndArchivedFalse(orderUnit.id ?: 0L)
        if (existingInspection != null) {
            if (existingInspection.status == InspectionStatus.SUBMITTED) {
                throw BadRequestException("Inspection already submitted for this unit")
            }
            return existingInspection.toDraftResponse()
        }

        val inspection = Inspection().apply {
            verificationOrder = orderUnit.verificationOrder
            this.orderUnit = orderUnit
            this.technician = technician
            template = requireCurrentTemplate()
            status = InspectionStatus.DRAFT
            startedAt = Instant.now(clock)
            lastSectionIndex = 0
        }
        orderUnit.verificationOrder.status = VerificationOrderStatus.IN_PROGRESS
        val saved = inspectionRepository.save(inspection)
        auditService.log(
            actor = technician,
            action = "CREATE_INSPECTION_DRAFT",
            entityName = "INSPECTION",
            entityId = saved.id.toString(),
            details = mapOf(
                "orderUnitId" to (orderUnit.id ?: 0L),
                "vehicleUnitId" to (orderUnit.vehicleUnit.id ?: 0L),
                "source" to if (request.orderUnitId != null) "ASSIGNED_ORDER" else "DIRECT_VEHICLE"
            )
        )
        return saved.toDraftResponse()
    }

    @Transactional(readOnly = true)
    fun getDraftInspection(technicianId: Long, inspectionId: Long): InspectionDraftResponse =
        requireAvailableInspection(technicianId, inspectionId).toDraftResponse()

    @Transactional
    fun updateDraftInspection(
        technicianId: Long,
        inspectionId: Long,
        request: UpdateInspectionRequest
    ): InspectionDraftResponse {
        val inspection = requireAvailableInspection(technicianId, inspectionId).also {
            it.status = InspectionStatus.DRAFT
        }
        val sectionsById = inspection.template.sections.filter { !it.archived }.associateBy { it.id ?: 0L }
        val questionsById = inspection.template.sections
            .flatMap { it.questions }
            .filter { !it.archived }
            .associateBy { it.id ?: 0L }

        val answerMap = inspection.answers.associateBy { it.question.id ?: 0L }.toMutableMap()
        val noteMap = inspection.sectionNotes.associateBy { it.section.id ?: 0L }.toMutableMap()

        request.sections.forEach { sectionRequest ->
            val section = sectionsById[sectionRequest.sectionId]
                ?: throw BadRequestException("Section ${sectionRequest.sectionId} does not belong to the active template")

            if (sectionRequest.note != null) {
                val note = noteMap[sectionRequest.sectionId] ?: InspectionSectionNote().apply {
                    this.inspection = inspection
                    this.section = section
                    inspection.sectionNotes += this
                }
                note.comment = sectionRequest.note
            }

            sectionRequest.questions.forEach { questionRequest ->
                val question = questionsById[questionRequest.questionId]
                    ?: throw BadRequestException("Question ${questionRequest.questionId} does not belong to the active template")

                if (question.section.id != section.id) {
                    throw BadRequestException("Question ${question.id} does not belong to section ${section.id}")
                }

                val answer = answerMap[questionRequest.questionId] ?: InspectionAnswer().apply {
                    this.inspection = inspection
                    this.question = question
                    inspection.answers += this
                }
                answer.answerValue = questionRequest.answer
                answer.comment = questionRequest.comment
            }
        }

        inspection.lastSectionIndex = request.lastSectionIndex
        inspection.overallComment = request.overallComment

        auditService.log(
            actor = inspection.technician,
            action = "UPDATE_INSPECTION_DRAFT",
            entityName = "INSPECTION",
            entityId = inspection.id.toString(),
            details = mapOf(
                "lastSectionIndex" to request.lastSectionIndex,
                "sectionsUpdated" to request.sections.size
            )
        )

        return inspection.toDraftResponse()
    }

    @Transactional
    fun addEvidence(
        technicianId: Long,
        inspectionId: Long,
        file: MultipartFile,
        sectionId: Long,
        comment: String?,
        capturedAt: Instant?
    ): InspectionDraftResponse {
        if (file.isEmpty) {
            throw BadRequestException("Evidence file cannot be empty")
        }

        val inspection = requireAvailableInspection(technicianId, inspectionId).also {
            it.status = InspectionStatus.DRAFT
        }
        val section = inspection.template.sections.firstOrNull { it.id == sectionId }
            ?: throw BadRequestException("Section $sectionId does not belong to the inspection template")

        val bytes = file.bytes
        val evidence = InspectionEvidence().apply {
            this.inspection = inspection
            this.section = section
            filename = file.originalFilename ?: "evidence-${Instant.now(clock).epochSecond}.jpg"
            mimeType = file.contentType ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
            checksum = sha256(bytes)
            this.capturedAt = capturedAt ?: Instant.now(clock)
            this.comment = comment
            content = bytes
        }

        inspection.evidences += evidence
        auditService.log(
            actor = inspection.technician,
            action = "ADD_INSPECTION_EVIDENCE",
            entityName = "INSPECTION",
            entityId = inspection.id.toString(),
            details = mapOf("filename" to evidence.filename, "sectionId" to section.id)
        )
        return inspection.toDraftResponse()
    }

    @Transactional
    fun deleteEvidence(
        technicianId: Long,
        inspectionId: Long,
        evidenceId: Long
    ): InspectionDraftResponse {
        val inspection = requireAvailableInspection(technicianId, inspectionId).also {
            it.status = InspectionStatus.DRAFT
        }
        val removed = inspection.evidences.removeIf { it.id == evidenceId }
        if (!removed) {
            throw NotFoundException("Evidence $evidenceId was not found")
        }
        auditService.log(
            actor = inspection.technician,
            action = "REMOVE_INSPECTION_EVIDENCE",
            entityName = "INSPECTION",
            entityId = inspection.id.toString(),
            details = mapOf("evidenceId" to evidenceId)
        )
        return inspection.toDraftResponse()
    }

    @Transactional
    fun pauseInspection(technicianId: Long, inspectionId: Long): InspectionDraftResponse {
        val inspection = requireAvailableInspection(technicianId, inspectionId)
        inspection.status = InspectionStatus.PAUSED
        auditService.log(
            actor = inspection.technician,
            action = "PAUSE_INSPECTION",
            entityName = "INSPECTION",
            entityId = inspection.id.toString()
        )
        return inspection.toDraftResponse()
    }

    @Transactional
    fun abandonInspection(technicianId: Long, inspectionId: Long) {
        val inspection = requireAvailableInspection(technicianId, inspectionId)
        inspection.archived = true

        val verificationOrderId = inspection.verificationOrder.id ?: 0L
        val totalUnits = orderUnitRepository.countByVerificationOrderIdAndArchivedFalse(verificationOrderId)
        val submittedForOrder = inspectionRepository.countByVerificationOrderIdAndStatusAndArchivedFalse(
            verificationOrderId,
            InspectionStatus.SUBMITTED
        )
        val activeInspections = inspectionRepository.countByVerificationOrderIdAndArchivedFalse(verificationOrderId)

        inspection.verificationOrder.status = when {
            submittedForOrder >= totalUnits -> VerificationOrderStatus.COMPLETED
            activeInspections > 0 -> VerificationOrderStatus.IN_PROGRESS
            else -> VerificationOrderStatus.OPEN
        }

        auditService.log(
            actor = inspection.technician,
            action = "ABANDON_INSPECTION",
            entityName = "INSPECTION",
            entityId = inspection.id.toString()
        )
    }

    @Transactional
    fun submitInspection(technicianId: Long, inspectionId: Long): SubmissionResponse {
        val inspection = requireAvailableInspection(technicianId, inspectionId)
        val requiredQuestions = inspection.template.sections
            .flatMap { it.questions }
            .filter { !it.archived && it.required }
        val answeredQuestionIds = inspection.answers.mapNotNull { it.question.id }.toSet()

        val missingQuestions = requiredQuestions.filter { question -> question.id !in answeredQuestionIds }
        if (missingQuestions.isNotEmpty()) {
            throw BadRequestException("Required questions are still unanswered")
        }

        inspection.status = InspectionStatus.SUBMITTED
        inspection.submittedAt = Instant.now(clock)
        inspection.overallResult = if (inspection.answers.any { it.answerValue == AnswerValue.FAIL }) {
            InspectionResult.FAIL
        } else {
            InspectionResult.PASS
        }

        val totalUnits = orderUnitRepository.countByVerificationOrderIdAndArchivedFalse(
            inspection.verificationOrder.id ?: 0L
        )
        val submittedForOrder = inspectionRepository.countByVerificationOrderIdAndStatusAndArchivedFalse(
            inspection.verificationOrder.id ?: 0L,
            InspectionStatus.SUBMITTED
        )
        inspection.verificationOrder.status = if (submittedForOrder >= totalUnits) {
            VerificationOrderStatus.COMPLETED
        } else {
            VerificationOrderStatus.IN_PROGRESS
        }

        merCompatibilityService.syncSubmittedInspection(inspection)

        auditService.log(
            actor = inspection.technician,
            action = "SUBMIT_INSPECTION",
            entityName = "INSPECTION",
            entityId = inspection.id.toString(),
            details = mapOf("overallResult" to inspection.overallResult, "evidenceCount" to inspection.evidences.size)
        )

        return SubmissionResponse(
            inspectionId = inspection.id ?: 0L,
            status = inspection.status,
            submittedAt = inspection.submittedAt
        )
    }

    private fun requireCurrentTemplate(): ChecklistTemplate =
        checklistTemplateRepository.findByCurrentTrueAndArchivedFalse()
            ?: throw NotFoundException("No active checklist template is configured")

    private fun requireTechnician(id: Long) =
        userRepository.findById(id).orElseThrow { NotFoundException("Technician $id was not found") }

    private fun requireClientCompany(id: Long) =
        clientCompanyRepository.findById(id)
            .orElseThrow { NotFoundException("Client company $id was not found") }

    private fun requireMobileVerificationOrder(
        technicianId: Long,
        verificationOrderId: Long?,
        clientCompanyId: Long
    ) = verificationOrderId?.let { id ->
        verificationOrderRepository.findById(id)
            .orElseThrow { NotFoundException("Verification order $id was not found") }
            .also {
                if (it.assignedTechnician.id != technicianId) {
                    throw ForbiddenException("Verification order is not assigned to this technician")
                }

                if (it.clientCompany.id != clientCompanyId) {
                    throw BadRequestException("Vehicle client does not match the selected verification order")
                }
            }
    }

    private fun requireMobileVehicle(technicianId: Long, vehicleId: Long): VehicleUnit {
        val vehicle = vehicleUnitRepository.findById(vehicleId)
            .orElseThrow { NotFoundException("Vehicle $vehicleId was not found") }

        if (vehicle.archived) {
            throw NotFoundException("Vehicle $vehicleId was not found")
        }

        val assignedToTechnician = orderUnitRepository.findAllAssignedToTechnician(technicianId)
            .any { orderUnit -> orderUnit.vehicleUnit.id == vehicleId }
        if (!assignedToTechnician) {
            throw ForbiddenException("Vehicle is not assigned to this technician")
        }

        return vehicle
    }

    private fun requireOrCreateMobileOrderUnit(technician: com.sivemor.platform.domain.User, vehicleUnitId: Long): com.sivemor.platform.domain.OrderUnit {
        val existingOrderUnit = orderUnitRepository
            .findAllAssignedToTechnicianByVehicleUnitId(technician.id ?: 0L, vehicleUnitId)
            .firstOrNull()
        if (existingOrderUnit != null) {
            return existingOrderUnit
        }

        val vehicle = vehicleUnitRepository.findById(vehicleUnitId)
            .orElseThrow { NotFoundException("Vehicle $vehicleUnitId was not found") }
        if (vehicle.archived) {
            throw NotFoundException("Vehicle $vehicleUnitId was not found")
        }

        val region = vehicle.clientCompany.region
            ?: throw BadRequestException("El cliente del vehiculo no tiene una region configurada")

        val verificationOrder = verificationOrderRepository.save(
            com.sivemor.platform.domain.VerificationOrder().apply {
                orderNumber = "MOV-${vehicle.id ?: vehicleUnitId}-${Instant.now(clock).epochSecond}"
                clientCompany = vehicle.clientCompany
                this.region = region
                assignedTechnician = technician
                status = VerificationOrderStatus.IN_PROGRESS
                scheduledAt = Instant.now(clock)
                notes = "AUTO_GENERATED_MOBILE"
            }
        )

        return orderUnitRepository.save(
            com.sivemor.platform.domain.OrderUnit().apply {
                this.verificationOrder = verificationOrder
                this.vehicleUnit = vehicle
            }
        )
    }

    private fun validateVehicleUniqueness(
        plate: String,
        vin: String,
        currentVehicleId: Long? = null
    ) {
        val plateOwner = vehicleUnitRepository.findByPlateIgnoreCaseAndArchivedFalse(plate)
        if (plateOwner != null && plateOwner.id != currentVehicleId) {
            throw BadRequestException("Ya existe un vehiculo con esa placa")
        }

        val vinOwner = vehicleUnitRepository.findByVinIgnoreCaseAndArchivedFalse(vin)
        if (vinOwner != null && vinOwner.id != currentVehicleId) {
            throw BadRequestException("Ya existe un vehiculo con ese VIN")
        }
    }

    private fun requireAvailableInspection(technicianId: Long, inspectionId: Long): Inspection {
        val inspection = inspectionRepository.findDetailedById(inspectionId)
            ?: throw NotFoundException("Inspection $inspectionId was not found")
        inspection.initializeDetails()

        if (inspection.archived ||
            inspection.technician.id != technicianId ||
            inspection.status == InspectionStatus.SUBMITTED
        ) {
            throw ForbiddenException("Inspection draft is not available")
        }
        return inspection
    }

    private fun Inspection.initializeDetails() {
        Hibernate.initialize(template.sections)
        template.sections.forEach { Hibernate.initialize(it.questions) }
        Hibernate.initialize(answers)
        answers.forEach { Hibernate.initialize(it.question) }
        Hibernate.initialize(sectionNotes)
        sectionNotes.forEach { Hibernate.initialize(it.section) }
        Hibernate.initialize(evidences)
        evidences.forEach { Hibernate.initialize(it.section) }
    }

    private fun ChecklistTemplate.toChecklistResponse(): ChecklistTemplateResponse = ChecklistTemplateResponse(
        id = id ?: 0L,
        name = name,
        version = version,
        sections = sections
            .filter { !it.archived }
            .sortedBy { it.displayOrder }
            .map { it.toChecklistSectionResponse() }
    )

    private fun ChecklistSection.toChecklistSectionResponse(): ChecklistSectionResponse = ChecklistSectionResponse(
        id = id ?: 0L,
        title = title,
        description = description,
        displayOrder = displayOrder,
        questions = questions
            .filter { !it.archived }
            .sortedBy { it.displayOrder }
            .map { it.toChecklistQuestionResponse() }
    )

    private fun ChecklistQuestion.toChecklistQuestionResponse(): ChecklistQuestionResponse = ChecklistQuestionResponse(
        id = id ?: 0L,
        code = code,
        prompt = prompt,
        required = required,
        displayOrder = displayOrder
    )

    private fun VehicleUnit.toMobileVehicleResponse(): MobileVehicleResponse = MobileVehicleResponse(
        id = id ?: 0L,
        clientCompanyId = clientCompany.id ?: 0L,
        clientCompanyName = clientCompany.name,
        plate = plate,
        vin = vin,
        category = category,
        brand = brand,
        model = model
    )

    private fun Inspection.toDraftResponse(): InspectionDraftResponse {
        val sections = template.sections
            .filter { !it.archived }
            .sortedBy { it.displayOrder }
            .map { section ->
                val sectionAnswers = answers.filter { it.question.section.id == section.id }
                val note = sectionNotes.firstOrNull { it.section.id == section.id }?.comment
                val evidencesForSection = evidences.filter { it.section?.id == section.id }
                InspectionSectionDraftResponse(
                    sectionId = section.id ?: 0L,
                    title = section.title,
                    description = section.description,
                    displayOrder = section.displayOrder,
                    note = note,
                    questions = section.questions
                        .filter { !it.archived }
                        .sortedBy { it.displayOrder }
                        .map { it.toChecklistQuestionResponse() },
                    answers = sectionAnswers.map {
                        InspectionQuestionAnswerResponse(
                            questionId = it.question.id ?: 0L,
                            answer = it.answerValue,
                            comment = it.comment
                        )
                    },
                    evidences = evidencesForSection.map {
                        EvidenceResponse(
                            id = it.id ?: 0L,
                            sectionId = it.section?.id ?: 0L,
                            filename = it.filename,
                            mimeType = it.mimeType,
                            capturedAt = it.capturedAt,
                            comment = it.comment
                        )
                    }
                )
            }

        return InspectionDraftResponse(
            id = id ?: 0L,
            orderId = verificationOrder.id ?: 0L,
            orderUnitId = orderUnit.id ?: 0L,
            orderNumber = verificationOrder.orderNumber,
            vehiclePlate = orderUnit.vehicleUnit.plate,
            clientCompanyName = verificationOrder.clientCompany.name,
            status = status,
            lastSectionIndex = lastSectionIndex,
            overallComment = overallComment,
            startedAt = startedAt,
            updatedAt = updatedAt,
            evidenceCount = evidences.size,
            sections = sections
        )
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
