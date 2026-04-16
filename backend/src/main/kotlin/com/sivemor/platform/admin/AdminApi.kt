package com.sivemor.platform.admin

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.common.NotFoundException
import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.Cedis
import com.sivemor.platform.domain.CedisRepository
import com.sivemor.platform.domain.ClientCompany
import com.sivemor.platform.domain.ClientCompanyRepository
import com.sivemor.platform.domain.Inspection
import com.sivemor.platform.domain.InspectionRepository
import com.sivemor.platform.domain.InspectionResult
import com.sivemor.platform.domain.InspectionStatus
import com.sivemor.platform.domain.OrderUnit
import com.sivemor.platform.domain.OrderUnitRepository
import com.sivemor.platform.domain.Payment
import com.sivemor.platform.domain.PaymentRepository
import com.sivemor.platform.domain.PaymentStatus
import com.sivemor.platform.domain.PaymentType
import com.sivemor.platform.domain.EvaluacionRepository
import com.sivemor.platform.domain.PhysicalDocumentOrder
import com.sivemor.platform.domain.PhysicalDocumentOrderRepository
import com.sivemor.platform.domain.PhysicalDocumentOrderStatus
import com.sivemor.platform.domain.Region
import com.sivemor.platform.domain.RegionRepository
import com.sivemor.platform.domain.Role
import com.sivemor.platform.domain.User
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.domain.VerificacionRepository
import com.sivemor.platform.domain.VerificacionVeredicto
import com.sivemor.platform.domain.VerificationCenter
import com.sivemor.platform.domain.VerificationCenterRepository
import com.sivemor.platform.domain.VehicleCategory
import com.sivemor.platform.domain.VehicleUnit
import com.sivemor.platform.domain.VehicleUnitRepository
import com.sivemor.platform.domain.VerificationOrder
import com.sivemor.platform.domain.VerificationOrderRepository
import com.sivemor.platform.domain.VerificationOrderStatus
import com.sivemor.platform.security.AppUserPrincipal
import com.sivemor.platform.service.AuditService
import com.sivemor.platform.service.PasswordGenerator
import com.sivemor.platform.service.UserCredentialMailer
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.Instant

data class UserUpsertRequest(
    @field:NotBlank @field:Size(max = 100) val username: String,
    @field:Email @field:Size(max = 150) val email: String,
    @field:NotBlank @field:Size(max = 160) val fullName: String,
    @field:NotNull val role: Role,
    val active: Boolean? = true
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val role: Role,
    val active: Boolean
)

data class PasswordResetResponse(
    val message: String
)

data class RegionUpsertRequest(
    @field:NotBlank @field:Size(max = 120) val name: String
)

data class RegionResponse(
    val id: Long,
    val name: String
)

data class ClientUpsertRequest(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:NotBlank @field:Size(max = 160) val businessName: String,
    @field:Email @field:Size(max = 150) val email: String,
    @field:NotBlank @field:Size(max = 30) val phone: String,
    @field:NotBlank @field:Size(max = 30) val alternatePhone: String,
    @field:NotBlank @field:Size(max = 160) val manager: String
)

data class ClientResponse(
    val id: Long,
    val name: String,
    val businessName: String,
    val email: String,
    val phone: String,
    val alternatePhone: String,
    val manager: String,
    val regionId: Long?,
    val regionName: String?
)

data class CedisUpsertRequest(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:Email @field:Size(max = 150) val email: String,
    @field:NotBlank @field:Size(max = 30) val phone: String,
    @field:NotBlank @field:Size(max = 30) val alternatePhone: String
)

data class CedisResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String,
    val alternatePhone: String
)

data class VerificationCenterUpsertRequest(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:NotBlank @field:Size(max = 60) val centerKey: String,
    @field:NotBlank @field:Size(max = 255) val address: String,
    @field:NotNull val regionId: Long,
    @field:NotBlank @field:Size(max = 160) val manager: String,
    @field:Email @field:Size(max = 150) val email: String,
    @field:NotBlank @field:Size(max = 30) val phone: String,
    @field:NotBlank @field:Size(max = 30) val alternatePhone: String,
    @field:NotBlank @field:Size(max = 120) val schedule: String
)

data class VerificationCenterResponse(
    val id: Long,
    val name: String,
    val centerKey: String,
    val address: String,
    val regionId: Long,
    val regionName: String,
    val manager: String,
    val email: String,
    val phone: String,
    val alternatePhone: String,
    val schedule: String
)

data class VehicleUpsertRequest(
    @field:NotNull val clientCompanyId: Long,
    @field:NotBlank @field:Size(max = 20) val plate: String,
    @field:NotBlank @field:Size(max = 30) val vin: String,
    @field:NotNull val category: VehicleCategory,
    @field:NotBlank @field:Size(max = 80) val brand: String,
    @field:NotBlank @field:Size(max = 80) val model: String
)

data class VehicleResponse(
    val id: Long,
    val clientCompanyId: Long,
    val clientCompanyName: String,
    val plate: String,
    val vin: String,
    val category: VehicleCategory,
    val brand: String,
    val model: String
)

data class OrderUpsertRequest(
    @field:NotBlank @field:Size(max = 50) val orderNumber: String,
    @field:NotNull val clientCompanyId: Long,
    @field:NotNull val regionId: Long,
    @field:NotNull val assignedTechnicianId: Long,
    @field:NotNull @field:NotEmpty val unitIds: List<Long>,
    @field:NotNull val scheduledAt: Instant,
    val notes: String? = null,
    val status: VerificationOrderStatus? = null
)

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val clientCompanyId: Long,
    val clientCompanyName: String,
    val regionId: Long,
    val regionName: String,
    val assignedTechnicianId: Long,
    val assignedTechnicianName: String,
    val status: VerificationOrderStatus,
    val scheduledAt: Instant,
    val notes: String?,
    val units: List<OrderUnitSummary>
)

data class OrderUnitSummary(
    val id: Long,
    val vehicleUnitId: Long,
    val vehiclePlate: String,
    val vehicleCategory: VehicleCategory
)

data class PaymentUpsertRequest(
    @field:NotNull val verificationOrderId: Long,
    @field:NotNull val paymentType: PaymentType,
    @field:DecimalMin("0.00") val amount: BigDecimal,
    @field:NotNull val status: PaymentStatus,
    @field:Size(max = 160) val depositAccount: String? = null,
    @field:Size(max = 120) val invoiceNumber: String? = null,
    val paidAt: Instant? = null
)

data class PaymentResponse(
    val id: Long,
    val verificationOrderId: Long,
    val orderNumber: String,
    val paymentType: PaymentType,
    val amount: BigDecimal,
    val status: PaymentStatus,
    val depositAccount: String?,
    val invoiceNumber: String?,
    val paidAt: Instant?
)

data class PhysicalDocumentOrderUpsertRequest(
    @field:NotNull val verificationOrderId: Long,
    @field:NotNull val shippedAt: Instant,
    @field:Size(max = 120) val trackingNumber: String? = null,
    @field:NotNull val status: PhysicalDocumentOrderStatus,
    @field:Size(max = 160) val receivedBy: String? = null,
    val photoData: String? = null,
    val comment: String? = null
)

data class PhysicalDocumentOrderResponse(
    val id: Long,
    val verificationOrderId: Long,
    val noteNumber: String,
    val shippedAt: Instant,
    val trackingNumber: String?,
    val status: PhysicalDocumentOrderStatus,
    val receivedBy: String?,
    val photoData: String?,
    val comment: String?
)

data class ReportSummaryResponse(
    val inspectionId: Long,
    val orderNumber: String,
    val clientCompanyName: String,
    val regionName: String,
    val technicianName: String,
    val vehiclePlate: String,
    val vehicleCategory: VehicleCategory,
    val submittedAt: Instant?,
    val overallResult: InspectionResult?,
    val failureCount: Int,
    val evidenceCount: Int
)

data class EvaluationDetailResponse(
    val verificacionId: Long?,
    val inspectionId: Long,
    val source: String,
    val orderNumber: String,
    val clientCompanyName: String,
    val regionName: String,
    val technicianName: String,
    val vehiclePlate: String,
    val vehicleCategory: VehicleCategory,
    val submittedAt: Instant?,
    val overallResult: String?,
    val overallComment: String?,
    val evidenceCount: Int,
    val sections: Map<String, Map<String, Any?>>
)

data class WebVerificationListItemResponse(
    val verificacionId: Long,
    val inspectionId: Long,
    val vehiclePlate: String,
    val clientCompanyName: String,
    val noteNumber: String,
    val approved: Boolean,
    val statusLabel: String,
    val submittedAt: Instant
)

data class WebVerificationUpdateRequest(
    val sections: Map<String, Map<String, String?>> = emptyMap()
)

data class FailureBucketResponse(
    val label: String,
    val count: Long
)

data class DashboardFailuresResponse(
    val totalSubmitted: Long,
    val totalFailed: Long,
    val unitsWithProblems: Long,
    val failuresByRegion: List<FailureBucketResponse>,
    val recentFailures: List<ReportSummaryResponse>
)

@RestController
@Tag(name = "Admin", description = "Administrative management and reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/admin")
class AdminController(
    private val adminService: AdminService
) {
    @Operation(summary = "List platform users")
    @GetMapping("/users")
    fun users(): List<UserResponse> = adminService.listUsers()

    @Operation(summary = "Create a platform user")
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: UserUpsertRequest
    ): UserResponse = adminService.createUser(principal.id, request)

    @Operation(summary = "Update a platform user")
    @PutMapping("/users/{id}")
    fun updateUser(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: UserUpsertRequest
    ): UserResponse = adminService.updateUser(principal.id, id, request)

    @Operation(summary = "Generate a new password for a platform user and send it by email")
    @PostMapping("/users/{id}/password/reset")
    fun resetUserPassword(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): PasswordResetResponse = adminService.resetUserPassword(principal.id, id)

    @Operation(summary = "Archive a platform user")
    @DeleteMapping("/users/{id}")
    fun deleteUser(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveUser(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "List regions")
    @GetMapping("/regions")
    fun regions(): List<RegionResponse> = adminService.listRegions()

    @Operation(summary = "Create a region")
    @PostMapping("/regions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRegion(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: RegionUpsertRequest
    ): RegionResponse = adminService.createRegion(principal.id, request)

    @Operation(summary = "Update a region")
    @PutMapping("/regions/{id}")
    fun updateRegion(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: RegionUpsertRequest
    ): RegionResponse = adminService.updateRegion(principal.id, id, request)

    @Operation(summary = "Archive a region")
    @DeleteMapping("/regions/{id}")
    fun deleteRegion(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveRegion(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "List client companies")
    @GetMapping("/clients")
    fun clients(): List<ClientResponse> = adminService.listClients()

    @Operation(summary = "Create a client company")
    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    fun createClient(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: ClientUpsertRequest
    ): ClientResponse = adminService.createClient(principal.id, request)

    @Operation(summary = "Get client company detail")
    @GetMapping("/clients/{id}")
    fun clientById(@PathVariable id: Long): ClientResponse = adminService.getClient(id)

    @Operation(summary = "Update a client company")
    @PutMapping("/clients/{id}")
    fun updateClient(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: ClientUpsertRequest
    ): ClientResponse = adminService.updateClient(principal.id, id, request)

    @Operation(summary = "Archive a client company")
    @DeleteMapping("/clients/{id}")
    fun deleteClient(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveClient(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "List vehicle units")
    @GetMapping("/vehicles")
    fun vehicles(): List<VehicleResponse> = adminService.listVehicles()

    @Operation(summary = "List CEDIS")
    @GetMapping("/cedis")
    fun cedis(): List<CedisResponse> = adminService.listCedis()

    @Operation(summary = "Get CEDIS detail")
    @GetMapping("/cedis/{id}")
    fun cedisById(@PathVariable id: Long): CedisResponse = adminService.getCedis(id)

    @Operation(summary = "Create a CEDIS")
    @PostMapping("/cedis")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCedis(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: CedisUpsertRequest
    ): CedisResponse = adminService.createCedis(principal.id, request)

    @Operation(summary = "Update a CEDIS")
    @PutMapping("/cedis/{id}")
    fun updateCedis(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: CedisUpsertRequest
    ): CedisResponse = adminService.updateCedis(principal.id, id, request)

    @Operation(summary = "Archive a CEDIS")
    @DeleteMapping("/cedis/{id}")
    fun deleteCedis(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveCedis(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "List verification centers")
    @GetMapping("/verification-centers")
    fun verificationCenters(): List<VerificationCenterResponse> = adminService.listVerificationCenters()

    @Operation(summary = "Get verification center detail")
    @GetMapping("/verification-centers/{id}")
    fun verificationCenterById(@PathVariable id: Long): VerificationCenterResponse = adminService.getVerificationCenter(id)

    @Operation(summary = "Create a verification center")
    @PostMapping("/verification-centers")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVerificationCenter(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: VerificationCenterUpsertRequest
    ): VerificationCenterResponse = adminService.createVerificationCenter(principal.id, request)

    @Operation(summary = "Update a verification center")
    @PutMapping("/verification-centers/{id}")
    fun updateVerificationCenter(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: VerificationCenterUpsertRequest
    ): VerificationCenterResponse = adminService.updateVerificationCenter(principal.id, id, request)

    @Operation(summary = "Archive a verification center")
    @DeleteMapping("/verification-centers/{id}")
    fun deleteVerificationCenter(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveVerificationCenter(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Create a vehicle unit")
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVehicle(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: VehicleUpsertRequest
    ): VehicleResponse = adminService.createVehicle(principal.id, request)

    @Operation(summary = "Update a vehicle unit")
    @PutMapping("/vehicles/{id}")
    fun updateVehicle(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: VehicleUpsertRequest
    ): VehicleResponse = adminService.updateVehicle(principal.id, id, request)

    @Operation(summary = "Archive a vehicle unit")
    @DeleteMapping("/vehicles/{id}")
    fun deleteVehicle(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveVehicle(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "List verification orders")
    @GetMapping("/orders")
    fun orders(): List<OrderResponse> = adminService.listOrders()

    @Operation(summary = "Create a verification order")
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: OrderUpsertRequest
    ): OrderResponse = adminService.createOrder(principal.id, request)

    @Operation(summary = "Update a verification order")
    @PutMapping("/orders/{id}")
    fun updateOrder(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: OrderUpsertRequest
    ): OrderResponse = adminService.updateOrder(principal.id, id, request)

    @Operation(summary = "Archive a verification order")
    @DeleteMapping("/orders/{id}")
    fun deleteOrder(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveOrder(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "List payment records")
    @GetMapping("/payments")
    fun payments(): List<PaymentResponse> = adminService.listPayments()

    @Operation(summary = "Get a payment record detail")
    @GetMapping("/payments/{id}")
    fun paymentById(@PathVariable id: Long): PaymentResponse = adminService.getPayment(id)

    @Operation(summary = "Create a payment record")
    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPayment(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: PaymentUpsertRequest
    ): PaymentResponse = adminService.createPayment(principal.id, request)

    @Operation(summary = "Update a payment record")
    @PutMapping("/payments/{id}")
    fun updatePayment(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PaymentUpsertRequest
    ): PaymentResponse = adminService.updatePayment(principal.id, id, request)

    @Operation(summary = "Archive a payment record")
    @DeleteMapping("/payments/{id}")
    fun deletePayment(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archivePayment(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "List physical document orders")
    @GetMapping("/physical-document-orders")
    fun physicalDocumentOrders(): List<PhysicalDocumentOrderResponse> = adminService.listPhysicalDocumentOrders()

    @Operation(summary = "Get a physical document order detail")
    @GetMapping("/physical-document-orders/{id}")
    fun physicalDocumentOrderById(@PathVariable id: Long): PhysicalDocumentOrderResponse =
        adminService.getPhysicalDocumentOrder(id)

    @Operation(summary = "Create a physical document order")
    @PostMapping("/physical-document-orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPhysicalDocumentOrder(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: PhysicalDocumentOrderUpsertRequest
    ): PhysicalDocumentOrderResponse = adminService.createPhysicalDocumentOrder(principal.id, request)

    @Operation(summary = "Update a physical document order")
    @PutMapping("/physical-document-orders/{id}")
    fun updatePhysicalDocumentOrder(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PhysicalDocumentOrderUpsertRequest
    ): PhysicalDocumentOrderResponse = adminService.updatePhysicalDocumentOrder(principal.id, id, request)

    @Operation(summary = "Archive a physical document order")
    @DeleteMapping("/physical-document-orders/{id}")
    fun deletePhysicalDocumentOrder(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archivePhysicalDocumentOrder(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @Operation(summary = "Query submitted reports with administrative filters")
    @GetMapping("/reports")
    fun reports(
        @RequestParam(required = false) companyId: Long?,
        @RequestParam(required = false) regionId: Long?,
        @RequestParam(required = false) orderId: Long?,
        @RequestParam(required = false) technicianId: Long?,
        @RequestParam(required = false) vehicleId: Long?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: Instant?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: Instant?,
        @RequestParam(defaultValue = "true") onlyFailures: Boolean
    ): List<ReportSummaryResponse> = adminService.listReports(
        companyId = companyId,
        regionId = regionId,
        orderId = orderId,
        technicianId = technicianId,
        vehicleId = vehicleId,
        from = from,
        to = to,
        onlyFailures = onlyFailures
    )

    @Operation(summary = "Get detailed report information for an inspection")
    @GetMapping("/reports/{inspectionId}")
    fun reportDetail(@PathVariable inspectionId: Long): EvaluationDetailResponse =
        adminService.getReportDetail(inspectionId)

    @Operation(summary = "List web verifications from the new MER-compatible store")
    @GetMapping("/web-verifications")
    fun webVerifications(): List<WebVerificationListItemResponse> = adminService.listWebVerifications()

    @Operation(summary = "Get a web verification detail by verification id")
    @GetMapping("/web-verifications/{id}")
    fun webVerificationDetail(@PathVariable id: Long): EvaluationDetailResponse =
        adminService.getWebVerificationDetail(id)

    @Operation(summary = "Update a web verification detail by verification id")
    @PutMapping("/web-verifications/{id}")
    fun updateWebVerificationDetail(
        @Parameter(hidden = true)
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @RequestBody request: WebVerificationUpdateRequest
    ): EvaluationDetailResponse = adminService.updateWebVerificationDetail(principal.id, id, request)

    @Operation(summary = "Return failure-focused dashboard metrics")
    @GetMapping("/dashboard/failures")
    fun dashboardFailures(): DashboardFailuresResponse = adminService.dashboardFailures()
}

@org.springframework.stereotype.Service
class AdminService(
    private val userRepository: UserRepository,
    private val regionRepository: RegionRepository,
    private val clientCompanyRepository: ClientCompanyRepository,
    private val cedisRepository: CedisRepository,
    private val verificationCenterRepository: VerificationCenterRepository,
    private val vehicleUnitRepository: VehicleUnitRepository,
    private val verificationOrderRepository: VerificationOrderRepository,
    private val orderUnitRepository: OrderUnitRepository,
    private val paymentRepository: PaymentRepository,
    private val physicalDocumentOrderRepository: PhysicalDocumentOrderRepository,
    private val inspectionRepository: InspectionRepository,
    private val verificacionRepository: VerificacionRepository,
    private val evaluacionRepository: EvaluacionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val auditService: AuditService,
    private val passwordGenerator: PasswordGenerator,
    private val userCredentialMailer: UserCredentialMailer
) {
    @Transactional(readOnly = true)
    fun listUsers(): List<UserResponse> = userRepository.findAllByArchivedFalseOrderByFullNameAsc().map { it.toResponse() }

    @Transactional
    fun createUser(actorId: Long, request: UserUpsertRequest): UserResponse {
        if (userRepository.findByUsernameIgnoreCaseAndArchivedFalse(request.username.trim()) != null) {
            throw BadRequestException("Username already exists")
        }

        val generatedPassword = passwordGenerator.generate()
        val user = User().apply {
            username = request.username.trim()
            email = request.email.trim().lowercase()
            fullName = request.fullName.trim()
            role = request.role
            active = request.active ?: true
            passwordHash = passwordEncoder.encode(generatedPassword) ?: throw IllegalStateException("Password encoder returned null")
        }

        val savedUser = userRepository.save(user)
        userCredentialMailer.sendNewPassword(savedUser, generatedPassword)
        return savedUser.alsoSaved(actorId, "CREATE_USER", user.fullName).toResponse()
    }

    @Transactional
    fun updateUser(actorId: Long, id: Long, request: UserUpsertRequest): UserResponse {
        val user = requireUser(id)
        user.username = request.username.trim()
        user.email = request.email.trim().lowercase()
        user.fullName = request.fullName.trim()
        user.role = request.role
        user.active = request.active ?: true
        return user.alsoSaved(actorId, "UPDATE_USER", user.fullName).toResponse()
    }

    @Transactional
    fun resetUserPassword(actorId: Long, id: Long): PasswordResetResponse {
        val user = requireUser(id)
        val generatedPassword = passwordGenerator.generate()
        user.passwordHash = passwordEncoder.encode(generatedPassword) ?: throw IllegalStateException("Password encoder returned null")
        userCredentialMailer.sendNewPassword(user, generatedPassword)
        logAction(actorId, "RESET_USER_PASSWORD", "USER", user.id.toString(), mapOf("username" to user.username))
        return PasswordResetResponse("La nueva contrasena fue generada y enviada al correo del usuario")
    }

    @Transactional
    fun archiveUser(actorId: Long, id: Long) {
        val user = requireUser(id)
        user.active = false
        user.archived = true
        logAction(actorId, "ARCHIVE_USER", "USER", user.id.toString(), mapOf("username" to user.username))
    }

    @Transactional(readOnly = true)
    fun listRegions(): List<RegionResponse> = regionRepository.findAllByArchivedFalseOrderByNameAsc().map { it.toResponse() }

    @Transactional
    fun createRegion(actorId: Long, request: RegionUpsertRequest): RegionResponse =
        regionRepository.save(Region().apply { name = request.name.trim() })
            .alsoSaved(actorId, "CREATE_REGION", request.name)
            .toResponse()

    @Transactional
    fun updateRegion(actorId: Long, id: Long, request: RegionUpsertRequest): RegionResponse =
        requireRegion(id)
            .apply { name = request.name.trim() }
            .alsoSaved(actorId, "UPDATE_REGION", request.name)
            .toResponse()

    @Transactional
    fun archiveRegion(actorId: Long, id: Long) {
        val region = requireRegion(id)
        region.archived = true
        logAction(actorId, "ARCHIVE_REGION", "REGION", region.id.toString(), mapOf("name" to region.name))
    }

    @Transactional(readOnly = true)
    fun listClients(): List<ClientResponse> = clientCompanyRepository.findAllByArchivedFalseOrderByNameAsc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getClient(id: Long): ClientResponse = requireClient(id).toResponse()

    @Transactional
    fun createClient(actorId: Long, request: ClientUpsertRequest): ClientResponse {
        val normalizedName = request.name.trim()
        if (clientCompanyRepository.findByNameIgnoreCaseAndArchivedFalse(normalizedName) != null) {
            throw BadRequestException("Ya existe un cliente con ese nombre")
        }

        return clientCompanyRepository.save(
            ClientCompany().apply {
                name = normalizedName
                businessName = request.businessName.trim()
                email = request.email.trim().lowercase()
                phone = digitsOnly(request.phone)
                alternatePhone = digitsOnly(request.alternatePhone)
                manager = request.manager.trim()
            }
        ).alsoSaved(actorId, "CREATE_CLIENT", request.name).toResponse()
    }

    @Transactional
    fun updateClient(actorId: Long, id: Long, request: ClientUpsertRequest): ClientResponse {
        val client = requireClient(id)
        val normalizedName = request.name.trim()
        val existing = clientCompanyRepository.findByNameIgnoreCaseAndArchivedFalse(normalizedName)
        if (existing != null && existing.id != client.id) {
            throw BadRequestException("Ya existe un cliente con ese nombre")
        }

        return client.apply {
            name = normalizedName
            businessName = request.businessName.trim()
            email = request.email.trim().lowercase()
            phone = digitsOnly(request.phone)
            alternatePhone = digitsOnly(request.alternatePhone)
            manager = request.manager.trim()
        }.alsoSaved(actorId, "UPDATE_CLIENT", request.name).toResponse()
    }

    @Transactional
    fun archiveClient(actorId: Long, id: Long) {
        val client = requireClient(id)
        client.archived = true
        logAction(actorId, "ARCHIVE_CLIENT", "CLIENT_COMPANY", client.id.toString(), mapOf("name" to client.name))
    }

    @Transactional(readOnly = true)
    fun listCedis(): List<CedisResponse> = cedisRepository.findAllByArchivedFalseOrderByNameAsc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getCedis(id: Long): CedisResponse = requireCedis(id).toResponse()

    @Transactional
    fun createCedis(actorId: Long, request: CedisUpsertRequest): CedisResponse =
        cedisRepository.save(
            Cedis().apply {
                name = request.name.trim()
                email = request.email.trim().lowercase()
                phone = request.phone.trim()
                alternatePhone = request.alternatePhone.trim()
            }
        ).alsoSaved(actorId, "CREATE_CEDIS", request.name).toResponse()

    @Transactional
    fun updateCedis(actorId: Long, id: Long, request: CedisUpsertRequest): CedisResponse =
        requireCedis(id).apply {
            name = request.name.trim()
            email = request.email.trim().lowercase()
            phone = request.phone.trim()
            alternatePhone = request.alternatePhone.trim()
        }.alsoSaved(actorId, "UPDATE_CEDIS", request.name).toResponse()

    @Transactional
    fun archiveCedis(actorId: Long, id: Long) {
        val cedis = requireCedis(id)
        cedis.archived = true
        logAction(actorId, "ARCHIVE_CEDIS", "CEDIS", cedis.id.toString(), mapOf("name" to cedis.name))
    }

    @Transactional(readOnly = true)
    fun listVerificationCenters(): List<VerificationCenterResponse> =
        verificationCenterRepository.findAllByArchivedFalseOrderByNameAsc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getVerificationCenter(id: Long): VerificationCenterResponse = requireVerificationCenter(id).toResponse()

    @Transactional
    fun createVerificationCenter(actorId: Long, request: VerificationCenterUpsertRequest): VerificationCenterResponse {
        val normalizedName = request.name.trim()
        val normalizedCenterKey = request.centerKey.trim().uppercase()

        if (verificationCenterRepository.findByNameIgnoreCaseAndArchivedFalse(normalizedName) != null) {
            throw BadRequestException("Ya existe un verificentro con ese nombre")
        }

        if (verificationCenterRepository.findByCenterKeyIgnoreCaseAndArchivedFalse(normalizedCenterKey) != null) {
            throw BadRequestException("Ya existe un verificentro con esa clave")
        }

        return verificationCenterRepository.save(
            VerificationCenter().apply {
                name = normalizedName
                centerKey = normalizedCenterKey
                address = request.address.trim()
                region = requireRegion(request.regionId)
                manager = request.manager.trim()
                email = request.email.trim().lowercase()
                phone = digitsOnly(request.phone)
                alternatePhone = digitsOnly(request.alternatePhone)
                schedule = request.schedule.trim()
            }
        ).alsoSaved(actorId, "CREATE_VERIFICATION_CENTER", request.name).toResponse()
    }

    @Transactional
    fun updateVerificationCenter(actorId: Long, id: Long, request: VerificationCenterUpsertRequest): VerificationCenterResponse {
        val verificationCenter = requireVerificationCenter(id)
        val normalizedName = request.name.trim()
        val normalizedCenterKey = request.centerKey.trim().uppercase()

        val existingByName = verificationCenterRepository.findByNameIgnoreCaseAndArchivedFalse(normalizedName)
        if (existingByName != null && existingByName.id != verificationCenter.id) {
            throw BadRequestException("Ya existe un verificentro con ese nombre")
        }

        val existingByCenterKey = verificationCenterRepository.findByCenterKeyIgnoreCaseAndArchivedFalse(normalizedCenterKey)
        if (existingByCenterKey != null && existingByCenterKey.id != verificationCenter.id) {
            throw BadRequestException("Ya existe un verificentro con esa clave")
        }

        return verificationCenter.apply {
            name = normalizedName
            centerKey = normalizedCenterKey
            address = request.address.trim()
            region = requireRegion(request.regionId)
            manager = request.manager.trim()
            email = request.email.trim().lowercase()
            phone = digitsOnly(request.phone)
            alternatePhone = digitsOnly(request.alternatePhone)
            schedule = request.schedule.trim()
        }.alsoSaved(actorId, "UPDATE_VERIFICATION_CENTER", request.name).toResponse()
    }

    @Transactional
    fun archiveVerificationCenter(actorId: Long, id: Long) {
        val verificationCenter = requireVerificationCenter(id)
        verificationCenter.archived = true
        logAction(
            actorId,
            "ARCHIVE_VERIFICATION_CENTER",
            "VERIFICATION_CENTER",
            verificationCenter.id.toString(),
            mapOf("name" to verificationCenter.name)
        )
    }

    @Transactional(readOnly = true)
    fun listVehicles(): List<VehicleResponse> = vehicleUnitRepository.findAllByArchivedFalseOrderByPlateAsc().map { it.toResponse() }

    @Transactional
    fun createVehicle(actorId: Long, request: VehicleUpsertRequest): VehicleResponse {
        val normalizedPlate = request.plate.trim().uppercase()
        val normalizedVin = request.vin.trim().uppercase()
        validateVehicleUniqueness(normalizedPlate, normalizedVin)

        return vehicleUnitRepository.save(
            VehicleUnit().apply {
                clientCompany = requireClient(request.clientCompanyId)
                plate = normalizedPlate
                vin = normalizedVin
                category = request.category
                brand = request.brand.trim()
                model = request.model.trim()
            }
        ).alsoSaved(actorId, "CREATE_VEHICLE", normalizedPlate).toResponse()
    }

    @Transactional
    fun updateVehicle(actorId: Long, id: Long, request: VehicleUpsertRequest): VehicleResponse {
        val normalizedPlate = request.plate.trim().uppercase()
        val normalizedVin = request.vin.trim().uppercase()
        validateVehicleUniqueness(normalizedPlate, normalizedVin, currentVehicleId = id)

        return requireVehicle(id).apply {
            clientCompany = requireClient(request.clientCompanyId)
            plate = normalizedPlate
            vin = normalizedVin
            category = request.category
            brand = request.brand.trim()
            model = request.model.trim()
        }.alsoSaved(actorId, "UPDATE_VEHICLE", normalizedPlate).toResponse()
    }

    @Transactional
    fun archiveVehicle(actorId: Long, id: Long) {
        val vehicle = requireVehicle(id)
        vehicle.archived = true
        logAction(actorId, "ARCHIVE_VEHICLE", "VEHICLE_UNIT", vehicle.id.toString(), mapOf("plate" to vehicle.plate))
    }

    @Transactional(readOnly = true)
    fun listOrders(): List<OrderResponse> = verificationOrderRepository.findAllByArchivedFalseOrderByScheduledAtDesc().map { it.toResponse() }

    @Transactional
    fun createOrder(actorId: Long, request: OrderUpsertRequest): OrderResponse {
        val technician = requireTechnician(request.assignedTechnicianId)
        val order = VerificationOrder().apply {
            orderNumber = request.orderNumber.trim().uppercase()
            clientCompany = requireClient(request.clientCompanyId)
            region = requireRegion(request.regionId)
            assignedTechnician = technician
            scheduledAt = request.scheduledAt
            notes = request.notes?.trim()
            status = request.status ?: VerificationOrderStatus.OPEN
        }
        val savedOrder = verificationOrderRepository.save(order)
        savedOrder.units.clear()
        savedOrder.units += request.unitIds.distinct().map { unitId ->
            OrderUnit().apply {
                verificationOrder = savedOrder
                vehicleUnit = requireVehicleForClient(unitId, savedOrder.clientCompany.id ?: 0L)
            }
        }
        return savedOrder.alsoSaved(actorId, "CREATE_ORDER", savedOrder.orderNumber).toResponse()
    }

    @Transactional
    fun updateOrder(actorId: Long, id: Long, request: OrderUpsertRequest): OrderResponse {
        val order = requireOrder(id)
        order.orderNumber = request.orderNumber.trim().uppercase()
        order.clientCompany = requireClient(request.clientCompanyId)
        order.region = requireRegion(request.regionId)
        order.assignedTechnician = requireTechnician(request.assignedTechnicianId)
        order.scheduledAt = request.scheduledAt
        order.notes = request.notes?.trim()
        order.status = request.status ?: order.status
        order.units.clear()
        order.units += request.unitIds.distinct().map { unitId ->
            OrderUnit().apply {
                verificationOrder = order
                vehicleUnit = requireVehicleForClient(unitId, order.clientCompany.id ?: 0L)
            }
        }
        return order.alsoSaved(actorId, "UPDATE_ORDER", order.orderNumber).toResponse()
    }

    @Transactional
    fun archiveOrder(actorId: Long, id: Long) {
        val order = requireOrder(id)
        order.archived = true
        logAction(actorId, "ARCHIVE_ORDER", "VERIFICATION_ORDER", order.id.toString(), mapOf("orderNumber" to order.orderNumber))
    }

    @Transactional(readOnly = true)
    fun listPayments(): List<PaymentResponse> = paymentRepository.findAllByArchivedFalseOrderByCreatedAtDesc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getPayment(id: Long): PaymentResponse = requirePayment(id).toResponse()

    @Transactional
    fun createPayment(actorId: Long, request: PaymentUpsertRequest): PaymentResponse =
        paymentRepository.save(
            Payment().apply {
                verificationOrder = requireOrder(request.verificationOrderId)
                paymentType = request.paymentType
                amount = request.amount
                status = request.status
                depositAccount = request.depositAccount?.trim()?.ifBlank { null }
                invoiceNumber = request.invoiceNumber?.trim()?.ifBlank { null }
                paidAt = request.paidAt
            }
        ).alsoSaved(actorId, "CREATE_PAYMENT", request.invoiceNumber ?: "payment").toResponse()

    @Transactional
    fun updatePayment(actorId: Long, id: Long, request: PaymentUpsertRequest): PaymentResponse =
        requirePayment(id).apply {
            verificationOrder = requireOrder(request.verificationOrderId)
            paymentType = request.paymentType
            amount = request.amount
            status = request.status
            depositAccount = request.depositAccount?.trim()?.ifBlank { null }
            invoiceNumber = request.invoiceNumber?.trim()?.ifBlank { null }
            paidAt = request.paidAt
        }.alsoSaved(actorId, "UPDATE_PAYMENT", request.invoiceNumber ?: "payment").toResponse()

    @Transactional
    fun archivePayment(actorId: Long, id: Long) {
        val payment = requirePayment(id)
        payment.archived = true
        logAction(actorId, "ARCHIVE_PAYMENT", "PAYMENT", payment.id.toString(), mapOf("invoiceNumber" to payment.invoiceNumber))
    }

    @Transactional(readOnly = true)
    fun listPhysicalDocumentOrders(): List<PhysicalDocumentOrderResponse> =
        physicalDocumentOrderRepository.findAllByArchivedFalseOrderByShippedAtDesc().map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getPhysicalDocumentOrder(id: Long): PhysicalDocumentOrderResponse =
        requirePhysicalDocumentOrder(id).toResponse()

    @Transactional
    fun createPhysicalDocumentOrder(actorId: Long, request: PhysicalDocumentOrderUpsertRequest): PhysicalDocumentOrderResponse =
        physicalDocumentOrderRepository.save(
            PhysicalDocumentOrder().apply {
                verificationOrder = requireOrder(request.verificationOrderId)
                shippedAt = request.shippedAt
                trackingNumber = request.trackingNumber?.trim()?.ifBlank { null }
                status = request.status
                receivedBy = request.receivedBy?.trim()?.ifBlank { null }
                photoData = request.photoData?.trim()?.ifBlank { null }
                comment = request.comment?.trim()?.ifBlank { null }
            }
        ).alsoSaved(actorId, "CREATE_PHYSICAL_DOCUMENT_ORDER", request.trackingNumber ?: "pedido").toResponse()

    @Transactional
    fun updatePhysicalDocumentOrder(actorId: Long, id: Long, request: PhysicalDocumentOrderUpsertRequest): PhysicalDocumentOrderResponse =
        requirePhysicalDocumentOrder(id).apply {
            verificationOrder = requireOrder(request.verificationOrderId)
            shippedAt = request.shippedAt
            trackingNumber = request.trackingNumber?.trim()?.ifBlank { null }
            status = request.status
            receivedBy = request.receivedBy?.trim()?.ifBlank { null }
            photoData = request.photoData?.trim()?.ifBlank { null }
            comment = request.comment?.trim()?.ifBlank { null }
        }.alsoSaved(actorId, "UPDATE_PHYSICAL_DOCUMENT_ORDER", request.trackingNumber ?: "pedido").toResponse()

    @Transactional
    fun archivePhysicalDocumentOrder(actorId: Long, id: Long) {
        val documentOrder = requirePhysicalDocumentOrder(id)
        documentOrder.archived = true
        logAction(
            actorId,
            "ARCHIVE_PHYSICAL_DOCUMENT_ORDER",
            "PHYSICAL_DOCUMENT_ORDER",
            documentOrder.id.toString(),
            mapOf("noteNumber" to documentOrder.verificationOrder.orderNumber)
        )
    }

    @Transactional(readOnly = true)
    fun listReports(
        companyId: Long?,
        regionId: Long?,
        orderId: Long?,
        technicianId: Long?,
        vehicleId: Long?,
        from: Instant?,
        to: Instant?,
        onlyFailures: Boolean
    ): List<ReportSummaryResponse> {
        val reportByInspectionId = linkedMapOf<Long, ReportSummaryResponse>()

        verificacionRepository.findAllByArchivedFalseOrderByFechaVerificacionDesc()
            .filter { verificacion ->
                (companyId == null || verificacion.verificationOrder.clientCompany.id == companyId) &&
                    (regionId == null || verificacion.verificationOrder.region.id == regionId) &&
                    (orderId == null || verificacion.verificationOrder.id == orderId) &&
                    (technicianId == null || verificacion.inspection.technician.id == technicianId) &&
                    (vehicleId == null || verificacion.vehicleUnit.id == vehicleId) &&
                    (from == null || !verificacion.fechaVerificacion.isBefore(from)) &&
                    (to == null || !verificacion.fechaVerificacion.isAfter(to)) &&
                    (!onlyFailures || verificacion.veredicto == VerificacionVeredicto.REPROBADO)
            }
            .forEach { verificacion ->
                val report = verificacion.toReportSummary(evaluacionRepository.findByVerificacionIdAndArchivedFalse(verificacion.id ?: 0L))
                reportByInspectionId[report.inspectionId] = report
            }

        inspectionRepository.findAllByStatusAndArchivedFalseOrderBySubmittedAtDesc(InspectionStatus.SUBMITTED)
            .filterNot { reportByInspectionId.containsKey(it.id ?: 0L) }
            .filter { inspection ->
                (companyId == null || inspection.verificationOrder.clientCompany.id == companyId) &&
                    (regionId == null || inspection.verificationOrder.region.id == regionId) &&
                    (orderId == null || inspection.verificationOrder.id == orderId) &&
                    (technicianId == null || inspection.technician.id == technicianId) &&
                    (vehicleId == null || inspection.orderUnit.vehicleUnit.id == vehicleId) &&
                    (from == null || (inspection.submittedAt != null && !inspection.submittedAt!!.isBefore(from))) &&
                    (to == null || (inspection.submittedAt != null && !inspection.submittedAt!!.isAfter(to))) &&
                    (!onlyFailures || inspection.overallResult == InspectionResult.FAIL)
            }
            .forEach { inspection ->
                reportByInspectionId[inspection.id ?: 0L] = inspection.toReportSummary()
            }

        return reportByInspectionId.values.toList()
    }

    @Transactional(readOnly = true)
    fun getReportDetail(inspectionId: Long): EvaluationDetailResponse {
        val verificacion = verificacionRepository.findByInspectionIdAndArchivedFalse(inspectionId)
        if (verificacion != null) {
            val evaluacion = evaluacionRepository.findByVerificacionIdAndArchivedFalse(verificacion.id ?: 0L)
            return verificacion.toEvaluationDetail(evaluacion)
        }

        return (inspectionRepository.findDetailedById(inspectionId)
            ?: throw NotFoundException("Inspection $inspectionId was not found"))
            .toLegacyEvaluationDetail()
    }

    @Transactional(readOnly = true)
    fun listWebVerifications(): List<WebVerificationListItemResponse> =
        verificacionRepository.findAllByArchivedFalseOrderByFechaVerificacionDesc().map { verificacion ->
            WebVerificationListItemResponse(
                verificacionId = verificacion.id ?: 0L,
                inspectionId = verificacion.inspection.id ?: 0L,
                vehiclePlate = verificacion.vehicleUnit.plate,
                clientCompanyName = verificacion.verificationOrder.clientCompany.name,
                noteNumber = verificacion.verificationOrder.orderNumber,
                approved = verificacion.veredicto == VerificacionVeredicto.APROBADO,
                statusLabel = if (verificacion.veredicto == VerificacionVeredicto.APROBADO) "Aprobado" else "Reprobado",
                submittedAt = verificacion.fechaVerificacion
            )
        }

    @Transactional(readOnly = true)
    fun getWebVerificationDetail(id: Long): EvaluationDetailResponse {
        val verificacion = verificacionRepository.findById(id)
            .orElseThrow { NotFoundException("Verification $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Verification $id was not found")
                }
            }
        val evaluacion = evaluacionRepository.findByVerificacionIdAndArchivedFalse(id)
        return verificacion.toEvaluationDetail(evaluacion)
    }

    @Transactional
    fun updateWebVerificationDetail(
        actorId: Long,
        id: Long,
        request: WebVerificationUpdateRequest
    ): EvaluationDetailResponse {
        val verificacion = verificacionRepository.findById(id)
            .orElseThrow { NotFoundException("Verification $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Verification $id was not found")
                }
            }
        val evaluacion = evaluacionRepository.findByVerificacionIdAndArchivedFalse(id)
            ?: throw NotFoundException("Evaluation for verification $id was not found")

        evaluacion.applySectionUpdates(request.sections)

        val verdict = if (evaluacion.failureCount() > 0) VerificacionVeredicto.REPROBADO else VerificacionVeredicto.APROBADO
        verificacion.veredicto = verdict
        verificacion.overallComment = evaluacion.comentariosGenerales
        verificacion.inspection.overallComment = evaluacion.comentariosGenerales
        verificacion.inspection.overallResult = if (verdict == VerificacionVeredicto.APROBADO) InspectionResult.PASS else InspectionResult.FAIL

        logAction(
            actorId,
            "UPDATE_WEB_VERIFICATION",
            "VERIFICACION",
            verificacion.id.toString(),
            mapOf("inspectionId" to (verificacion.inspection.id ?: 0L))
        )

        return verificacion.toEvaluationDetail(evaluacion)
    }

    @Transactional(readOnly = true)
    fun dashboardFailures(): DashboardFailuresResponse {
        val submitted = listReports(
            companyId = null,
            regionId = null,
            orderId = null,
            technicianId = null,
            vehicleId = null,
            from = null,
            to = null,
            onlyFailures = false
        )
        val failed = submitted.filter {
            it.overallResult?.name == InspectionResult.FAIL.name || it.overallResult?.name == VerificacionVeredicto.REPROBADO.name
        }
        val failuresByRegion = failed
            .groupBy { it.regionName }
            .map { FailureBucketResponse(label = it.key, count = it.value.size.toLong()) }
            .sortedByDescending { it.count }

        return DashboardFailuresResponse(
            totalSubmitted = submitted.size.toLong(),
            totalFailed = failed.size.toLong(),
            unitsWithProblems = failed.map { it.vehiclePlate }.distinct().size.toLong(),
            failuresByRegion = failuresByRegion,
            recentFailures = failed.take(10)
        )
    }

    private fun User.toResponse(): UserResponse = UserResponse(
        id = id ?: 0L,
        username = username,
        email = email,
        fullName = fullName,
        role = role,
        active = active
    )

    private fun Region.toResponse(): RegionResponse = RegionResponse(
        id = id ?: 0L,
        name = name
    )

    private fun ClientCompany.toResponse(): ClientResponse = ClientResponse(
        id = id ?: 0L,
        name = name,
        businessName = businessName,
        email = email,
        phone = phone,
        alternatePhone = alternatePhone,
        manager = manager,
        regionId = region?.id,
        regionName = region?.name
    )

    private fun Cedis.toResponse(): CedisResponse = CedisResponse(
        id = id ?: 0L,
        name = name,
        email = email,
        phone = phone,
        alternatePhone = alternatePhone
    )

    private fun VerificationCenter.toResponse(): VerificationCenterResponse = VerificationCenterResponse(
        id = id ?: 0L,
        name = name,
        centerKey = centerKey,
        address = address,
        regionId = region.id ?: 0L,
        regionName = region.name,
        manager = manager,
        email = email,
        phone = phone,
        alternatePhone = alternatePhone,
        schedule = schedule
    )

    private fun VehicleUnit.toResponse(): VehicleResponse = VehicleResponse(
        id = id ?: 0L,
        clientCompanyId = clientCompany.id ?: 0L,
        clientCompanyName = clientCompany.name,
        plate = plate,
        vin = vin,
        category = category,
        brand = brand,
        model = model
    )

    private fun VerificationOrder.toResponse(): OrderResponse = OrderResponse(
        id = id ?: 0L,
        orderNumber = orderNumber,
        clientCompanyId = clientCompany.id ?: 0L,
        clientCompanyName = clientCompany.name,
        regionId = region.id ?: 0L,
        regionName = region.name,
        assignedTechnicianId = assignedTechnician.id ?: 0L,
        assignedTechnicianName = assignedTechnician.fullName,
        status = status,
        scheduledAt = scheduledAt,
        notes = notes,
        units = units.filter { !it.archived }.map {
            OrderUnitSummary(
                id = it.id ?: 0L,
                vehicleUnitId = it.vehicleUnit.id ?: 0L,
                vehiclePlate = it.vehicleUnit.plate,
                vehicleCategory = it.vehicleUnit.category
            )
        }
    )

    private fun Payment.toResponse(): PaymentResponse = PaymentResponse(
        id = id ?: 0L,
        verificationOrderId = verificationOrder.id ?: 0L,
        orderNumber = verificationOrder.orderNumber,
        paymentType = paymentType,
        amount = amount,
        status = status,
        depositAccount = depositAccount,
        invoiceNumber = invoiceNumber,
        paidAt = paidAt
    )

    private fun PhysicalDocumentOrder.toResponse(): PhysicalDocumentOrderResponse = PhysicalDocumentOrderResponse(
        id = id ?: 0L,
        verificationOrderId = verificationOrder.id ?: 0L,
        noteNumber = verificationOrder.orderNumber,
        shippedAt = shippedAt,
        trackingNumber = trackingNumber,
        status = status,
        receivedBy = receivedBy,
        photoData = photoData,
        comment = comment
    )

    private fun Inspection.toReportSummary(): ReportSummaryResponse = ReportSummaryResponse(
        inspectionId = id ?: 0L,
        orderNumber = verificationOrder.orderNumber,
        clientCompanyName = verificationOrder.clientCompany.name,
        regionName = verificationOrder.region.name,
        technicianName = technician.fullName,
        vehiclePlate = orderUnit.vehicleUnit.plate,
        vehicleCategory = orderUnit.vehicleUnit.category,
        submittedAt = submittedAt,
        overallResult = overallResult,
        failureCount = answers.count { it.answerValue == AnswerValue.FAIL },
        evidenceCount = evidences.size
    )

    private fun com.sivemor.platform.domain.Verificacion.toReportSummary(
        evaluacion: com.sivemor.platform.domain.Evaluacion?
    ): ReportSummaryResponse = ReportSummaryResponse(
        inspectionId = inspection.id ?: 0L,
        orderNumber = verificationOrder.orderNumber,
        clientCompanyName = verificationOrder.clientCompany.name,
        regionName = verificationOrder.region.name,
        technicianName = inspection.technician.fullName,
        vehiclePlate = vehicleUnit.plate,
        vehicleCategory = vehicleUnit.category,
        submittedAt = fechaVerificacion,
        overallResult = if (veredicto == VerificacionVeredicto.REPROBADO) InspectionResult.FAIL else InspectionResult.PASS,
        failureCount = evaluacion?.failureCount() ?: inspection.answers.count { it.answerValue == AnswerValue.FAIL },
        evidenceCount = evaluacion?.evidenceCount ?: inspection.evidences.size
    )

    private fun com.sivemor.platform.domain.Verificacion.toEvaluationDetail(
        evaluacion: com.sivemor.platform.domain.Evaluacion?
    ): EvaluationDetailResponse = EvaluationDetailResponse(
        verificacionId = id ?: 0L,
        inspectionId = inspection.id ?: 0L,
        source = "MER",
        orderNumber = verificationOrder.orderNumber,
        clientCompanyName = verificationOrder.clientCompany.name,
        regionName = verificationOrder.region.name,
        technicianName = inspection.technician.fullName,
        vehiclePlate = vehicleUnit.plate,
        vehicleCategory = vehicleUnit.category,
        submittedAt = fechaVerificacion,
        overallResult = veredicto.name,
        overallComment = overallComment,
        evidenceCount = evaluacion?.evidenceCount ?: inspection.evidences.size,
        sections = evaluacion.toSectionMap()
    )

    private fun Inspection.toLegacyEvaluationDetail(): EvaluationDetailResponse = EvaluationDetailResponse(
        verificacionId = null,
        inspectionId = id ?: 0L,
        source = "LEGACY",
        orderNumber = verificationOrder.orderNumber,
        clientCompanyName = verificationOrder.clientCompany.name,
        regionName = verificationOrder.region.name,
        technicianName = technician.fullName,
        vehiclePlate = orderUnit.vehicleUnit.plate,
        vehicleCategory = orderUnit.vehicleUnit.category,
        submittedAt = submittedAt,
        overallResult = overallResult?.name,
        overallComment = overallComment,
        evidenceCount = evidences.size,
        sections = template.sections
            .filter { !it.archived }
            .associate { section ->
                section.title to section.questions
                    .filter { !it.archived }
                    .associate { question ->
                        question.code to (answers.firstOrNull { it.question.id == question.id }?.answerValue?.name)
                    } + mapOf("comment" to sectionNotes.firstOrNull { it.section.id == section.id }?.comment)
            }
    )

    private fun com.sivemor.platform.domain.Evaluacion?.toSectionMap(): Map<String, Map<String, Any?>> {
        if (this == null) return emptyMap()
        return linkedMapOf(
            "luces" to linkedMapOf(
                "luces_galibo" to lucesGalibo,
                "luces_altas" to lucesAltas,
                "luces_bajas" to lucesBajas,
                "luces_demarcadoras_delanteras" to lucesDemarcadorasDelanteras,
                "luces_demarcadoras_traseras" to lucesDemarcadorasTraseras,
                "luces_indicadoras" to lucesIndicadoras,
                "faro_izquierdo" to faroIzquierdo,
                "faro_derecho" to faroDerecho,
                "luces_direccionales_delanteras" to lucesDireccionalesDelanteras,
                "luces_direccionales_traseras" to lucesDireccionalesTraseras,
                "comment" to comentarioLuces
            ),
            "llantas" to linkedMapOf(
                "llantas_rines_delanteros" to llantasRinesDelanteros,
                "llantas_rines_traseros" to llantasRinesTraseros,
                "llantas_masas_delanteras" to llantasMasasDelanteras,
                "llantas_masas_traseras" to llantasMasasTraseras,
                "llantas_presion_delantera_izquierda" to llantasPresionDelanteraIzquierda,
                "llantas_presion_delantera_derecha" to llantasPresionDelanteraDerecha,
                "llantas_presion_trasera_izquierda_1" to llantasPresionTraseraIzquierda1,
                "llantas_presion_trasera_izquierda_2" to llantasPresionTraseraIzquierda2,
                "llantas_presion_trasera_derecha_1" to llantasPresionTraseraDerecha1,
                "llantas_presion_trasera_derecha_2" to llantasPresionTraseraDerecha2,
                "llantas_profundidad_delantera_izquierda" to llantasProfundidadDelanteraIzquierda,
                "llantas_profundidad_delantera_derecha" to llantasProfundidadDelanteraDerecha,
                "llantas_profundidad_trasera_izquierda_1" to llantasProfundidadTraseraIzquierda1,
                "llantas_profundidad_trasera_izquierda_2" to llantasProfundidadTraseraIzquierda2,
                "llantas_profundidad_trasera_derecha_1" to llantasProfundidadTraseraDerecha1,
                "llantas_profundidad_trasera_derecha_2" to llantasProfundidadTraseraDerecha2,
                "llantas_tuercas_delantera_izquierda" to llantasTuercasDelanteraIzquierda,
                "llantas_tuercas_delantera_izquierda_faltantes" to llantasTuercasDelanteraIzquierdaFaltantes,
                "llantas_tuercas_delantera_izquierda_rotas" to llantasTuercasDelanteraIzquierdaRotas,
                "llantas_tuercas_delantera_derecha" to llantasTuercasDelanteraDerecha,
                "llantas_tuercas_delantera_derecha_faltantes" to llantasTuercasDelanteraDerechaFaltantes,
                "llantas_tuercas_delantera_derecha_rotas" to llantasTuercasDelanteraDerechaRotas,
                "llantas_tuercas_trasera_izquierda" to llantasTuercasTraseraIzquierda,
                "llantas_tuercas_trasera_izquierda_faltantes" to llantasTuercasTraseraIzquierdaFaltantes,
                "llantas_tuercas_trasera_izquierda_rotas" to llantasTuercasTraseraIzquierdaRotas,
                "llantas_tuercas_trasera_derecha" to llantasTuercasTraseraDerecha,
                "llantas_tuercas_trasera_derecha_faltantes" to llantasTuercasTraseraDerechaFaltantes,
                "llantas_tuercas_trasera_derecha_rotas" to llantasTuercasTraseraDerechaRotas,
                "comment" to comentarioLlantas
            ),
            "direccion" to linkedMapOf(
                "direccion_brazo_pitman" to direccionBrazoPitman,
                "direccion_manijas_puertas" to direccionManijasPuertas,
                "direccion_chavetas" to direccionChavetas,
                "direccion_chavetas_faltantes" to direccionChavetasFaltantes,
                "comment" to comentarioDireccion
            ),
            "aire_frenos" to linkedMapOf(
                "aire_frenos_compresor" to aireFrenosCompresor,
                "aire_frenos_tanques_aire" to aireFrenosTanquesAire,
                "aire_frenos_tiempo_carga_psi" to aireFrenosTiempoCargaPsi,
                "aire_frenos_tiempo_carga_tiempo" to aireFrenosTiempoCargaTiempo,
                "comment" to comentarioAireFrenos
            ),
            "motor_emisiones" to linkedMapOf(
                "motor_emisiones_humo" to motorEmisionesHumo,
                "motor_emisiones_gobernado" to motorEmisionesGobernado,
                "comment" to comentarioMotorEmisiones
            ),
            "otros" to linkedMapOf(
                "otros_caja_direccion" to otrosCajaDireccion,
                "otros_deposito_aceite" to otrosDepositoAceite,
                "otros_parabrisas" to otrosParabrisas,
                "otros_limpiaparabrisas" to otrosLimpiaparabrisas,
                "otros_juego" to otrosJuego,
                "otros_escape" to otrosEscape,
                "comment" to comentarioOtros
            ),
            "general" to linkedMapOf(
                "comentarios_generales" to comentariosGenerales,
                "evidence_count" to evidenceCount
            )
        )
    }

    private fun com.sivemor.platform.domain.Evaluacion.failureCount(): Int = listOfNotNull(
        lucesGalibo,
        lucesAltas,
        lucesBajas,
        lucesDemarcadorasDelanteras,
        lucesDemarcadorasTraseras,
        lucesIndicadoras,
        faroIzquierdo,
        faroDerecho,
        lucesDireccionalesDelanteras,
        lucesDireccionalesTraseras,
        llantasRinesDelanteros,
        llantasRinesTraseros,
        llantasMasasDelanteras,
        llantasMasasTraseras,
        llantasTuercasDelanteraIzquierda,
        llantasTuercasDelanteraDerecha,
        llantasTuercasTraseraIzquierda,
        llantasTuercasTraseraDerecha,
        direccionBrazoPitman,
        direccionManijasPuertas,
        direccionChavetas,
        aireFrenosCompresor,
        aireFrenosTanquesAire,
        motorEmisionesHumo,
        motorEmisionesGobernado,
        otrosCajaDireccion,
        otrosDepositoAceite,
        otrosParabrisas,
        otrosLimpiaparabrisas,
        otrosJuego,
        otrosEscape
    ).count { it == "REPROBADO" }

    private fun com.sivemor.platform.domain.Evaluacion.applySectionUpdates(
        updates: Map<String, Map<String, String?>>
    ) {
        updates.forEach { (sectionName, fields) ->
            fields.forEach { (field, rawValue) ->
                when (field) {
                    "luces_galibo" -> lucesGalibo = parseEnumValue(rawValue)
                    "luces_altas" -> lucesAltas = parseEnumValue(rawValue)
                    "luces_bajas" -> lucesBajas = parseEnumValue(rawValue)
                    "luces_demarcadoras_delanteras" -> lucesDemarcadorasDelanteras = parseEnumValue(rawValue)
                    "luces_demarcadoras_traseras" -> lucesDemarcadorasTraseras = parseEnumValue(rawValue)
                    "luces_indicadoras" -> lucesIndicadoras = parseEnumValue(rawValue)
                    "faro_izquierdo" -> faroIzquierdo = parseEnumValue(rawValue)
                    "faro_derecho" -> faroDerecho = parseEnumValue(rawValue)
                    "luces_direccionales_delanteras" -> lucesDireccionalesDelanteras = parseEnumValue(rawValue)
                    "luces_direccionales_traseras" -> lucesDireccionalesTraseras = parseEnumValue(rawValue)
                    "llantas_rines_delanteros" -> llantasRinesDelanteros = parseEnumValue(rawValue)
                    "llantas_rines_traseros" -> llantasRinesTraseros = parseEnumValue(rawValue)
                    "llantas_masas_delanteras" -> llantasMasasDelanteras = parseEnumValue(rawValue)
                    "llantas_masas_traseras" -> llantasMasasTraseras = parseEnumValue(rawValue)
                    "llantas_presion_delantera_izquierda" -> llantasPresionDelanteraIzquierda = parseDoubleValue(rawValue)
                    "llantas_presion_delantera_derecha" -> llantasPresionDelanteraDerecha = parseDoubleValue(rawValue)
                    "llantas_presion_trasera_izquierda_1" -> llantasPresionTraseraIzquierda1 = parseDoubleValue(rawValue)
                    "llantas_presion_trasera_izquierda_2" -> llantasPresionTraseraIzquierda2 = parseDoubleValue(rawValue)
                    "llantas_presion_trasera_derecha_1" -> llantasPresionTraseraDerecha1 = parseDoubleValue(rawValue)
                    "llantas_presion_trasera_derecha_2" -> llantasPresionTraseraDerecha2 = parseDoubleValue(rawValue)
                    "llantas_profundidad_delantera_izquierda" -> llantasProfundidadDelanteraIzquierda = parseDoubleValue(rawValue)
                    "llantas_profundidad_delantera_derecha" -> llantasProfundidadDelanteraDerecha = parseDoubleValue(rawValue)
                    "llantas_profundidad_trasera_izquierda_1" -> llantasProfundidadTraseraIzquierda1 = parseDoubleValue(rawValue)
                    "llantas_profundidad_trasera_izquierda_2" -> llantasProfundidadTraseraIzquierda2 = parseDoubleValue(rawValue)
                    "llantas_profundidad_trasera_derecha_1" -> llantasProfundidadTraseraDerecha1 = parseDoubleValue(rawValue)
                    "llantas_profundidad_trasera_derecha_2" -> llantasProfundidadTraseraDerecha2 = parseDoubleValue(rawValue)
                    "llantas_tuercas_delantera_izquierda" -> llantasTuercasDelanteraIzquierda = parseEnumValue(rawValue)
                    "llantas_tuercas_delantera_izquierda_faltantes" -> llantasTuercasDelanteraIzquierdaFaltantes = parseIntValue(rawValue)
                    "llantas_tuercas_delantera_izquierda_rotas" -> llantasTuercasDelanteraIzquierdaRotas = parseIntValue(rawValue)
                    "llantas_tuercas_delantera_derecha" -> llantasTuercasDelanteraDerecha = parseEnumValue(rawValue)
                    "llantas_tuercas_delantera_derecha_faltantes" -> llantasTuercasDelanteraDerechaFaltantes = parseIntValue(rawValue)
                    "llantas_tuercas_delantera_derecha_rotas" -> llantasTuercasDelanteraDerechaRotas = parseIntValue(rawValue)
                    "llantas_tuercas_trasera_izquierda" -> llantasTuercasTraseraIzquierda = parseEnumValue(rawValue)
                    "llantas_tuercas_trasera_izquierda_faltantes" -> llantasTuercasTraseraIzquierdaFaltantes = parseIntValue(rawValue)
                    "llantas_tuercas_trasera_izquierda_rotas" -> llantasTuercasTraseraIzquierdaRotas = parseIntValue(rawValue)
                    "llantas_tuercas_trasera_derecha" -> llantasTuercasTraseraDerecha = parseEnumValue(rawValue)
                    "llantas_tuercas_trasera_derecha_faltantes" -> llantasTuercasTraseraDerechaFaltantes = parseIntValue(rawValue)
                    "llantas_tuercas_trasera_derecha_rotas" -> llantasTuercasTraseraDerechaRotas = parseIntValue(rawValue)
                    "direccion_brazo_pitman" -> direccionBrazoPitman = parseEnumValue(rawValue)
                    "direccion_manijas_puertas" -> direccionManijasPuertas = parseEnumValue(rawValue)
                    "direccion_chavetas" -> direccionChavetas = parseEnumValue(rawValue)
                    "direccion_chavetas_faltantes" -> direccionChavetasFaltantes = parseIntValue(rawValue)
                    "aire_frenos_compresor" -> aireFrenosCompresor = parseEnumValue(rawValue)
                    "aire_frenos_tanques_aire" -> aireFrenosTanquesAire = parseEnumValue(rawValue)
                    "aire_frenos_tiempo_carga_psi" -> aireFrenosTiempoCargaPsi = parseDoubleValue(rawValue)
                    "aire_frenos_tiempo_carga_tiempo" -> aireFrenosTiempoCargaTiempo = parseDoubleValue(rawValue)
                    "motor_emisiones_humo" -> motorEmisionesHumo = parseEnumValue(rawValue)
                    "motor_emisiones_gobernado" -> motorEmisionesGobernado = parseEnumValue(rawValue)
                    "otros_caja_direccion" -> otrosCajaDireccion = parseEnumValue(rawValue)
                    "otros_deposito_aceite" -> otrosDepositoAceite = parseEnumValue(rawValue)
                    "otros_parabrisas" -> otrosParabrisas = parseEnumValue(rawValue)
                    "otros_limpiaparabrisas" -> otrosLimpiaparabrisas = parseEnumValue(rawValue)
                    "otros_juego" -> otrosJuego = parseEnumValue(rawValue)
                    "otros_escape" -> otrosEscape = parseEnumValue(rawValue)
                    "comment" -> applySectionComment(sectionName, rawValue)
                    "comentarios_generales" -> comentariosGenerales = normalizeText(rawValue)
                    "evidence_count" -> evidenceCount = parseIntValue(rawValue) ?: evidenceCount
                }
            }
        }
    }

    private fun com.sivemor.platform.domain.Evaluacion.applySectionComment(sectionName: String, value: String?) {
        when (sectionName) {
            "luces" -> comentarioLuces = normalizeText(value)
            "llantas" -> comentarioLlantas = normalizeText(value)
            "direccion" -> comentarioDireccion = normalizeText(value)
            "aire_frenos" -> comentarioAireFrenos = normalizeText(value)
            "motor_emisiones" -> comentarioMotorEmisiones = normalizeText(value)
            "otros" -> comentarioOtros = normalizeText(value)
            "general" -> comentariosGenerales = normalizeText(value)
        }
    }

    private fun parseEnumValue(value: String?): String? =
        normalizeText(value)?.uppercase()?.let {
            when (it) {
                "APROBADO", "REPROBADO", "NO_APLICA" -> it
                else -> throw BadRequestException("Invalid enum value: $it")
            }
        }

    private fun parseDoubleValue(value: String?): Double? {
        val normalized = normalizeText(value) ?: return null
        return normalized.replace(",", ".").toDoubleOrNull()
            ?: throw BadRequestException("Invalid numeric value: $normalized")
    }

    private fun parseIntValue(value: String?): Int? = parseDoubleValue(value)?.toInt()

    private fun normalizeText(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    private fun requireUser(id: Long): User =
        userRepository.findById(id).orElseThrow { NotFoundException("User $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("User $id was not found")
                }
            }

    private fun requireTechnician(id: Long): User = requireUser(id).also {
        if (it.role != Role.TECHNICIAN) {
            throw BadRequestException("Assigned user must be a technician")
        }
    }

    private fun requireRegion(id: Long): Region =
        regionRepository.findById(id).orElseThrow { NotFoundException("Region $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Region $id was not found")
                }
            }

    private fun requireClient(id: Long): ClientCompany =
        clientCompanyRepository.findById(id).orElseThrow { NotFoundException("Client $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Client $id was not found")
                }
            }

    private fun requireCedis(id: Long): Cedis =
        cedisRepository.findById(id).orElseThrow { NotFoundException("CEDIS $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("CEDIS $id was not found")
                }
            }

    private fun requireVerificationCenter(id: Long): VerificationCenter =
        verificationCenterRepository.findById(id).orElseThrow { NotFoundException("Verification center $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Verification center $id was not found")
                }
            }

    private fun requireVehicle(id: Long): VehicleUnit =
        vehicleUnitRepository.findById(id).orElseThrow { NotFoundException("Vehicle $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Vehicle $id was not found")
                }
            }

    private fun requireVehicleForClient(id: Long, clientId: Long): VehicleUnit =
        requireVehicle(id).also {
            if (it.clientCompany.id != clientId) {
                throw BadRequestException("Vehicle $id does not belong to client $clientId")
            }
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

    private fun digitsOnly(value: String): String = value.filter(Char::isDigit)

    private fun requireOrder(id: Long): VerificationOrder =
        verificationOrderRepository.findById(id).orElseThrow { NotFoundException("Order $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Order $id was not found")
                }
            }

    private fun requirePayment(id: Long): Payment =
        paymentRepository.findById(id).orElseThrow { NotFoundException("Payment $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Payment $id was not found")
                }
            }

    private fun requirePhysicalDocumentOrder(id: Long): PhysicalDocumentOrder =
        physicalDocumentOrderRepository.findById(id).orElseThrow { NotFoundException("Physical document order $id was not found") }
            .also {
                if (it.archived) {
                    throw NotFoundException("Physical document order $id was not found")
                }
            }

    private fun <T : Any> T.alsoSaved(actorId: Long, action: String, label: String): T {
        val actor = requireUser(actorId)
        val entityName = this::class.simpleName ?: "ENTITY"
        val entityId = when (this) {
            is User -> id.toString()
            is Region -> id.toString()
            is ClientCompany -> id.toString()
            is Cedis -> id.toString()
            is VerificationCenter -> id.toString()
            is VehicleUnit -> id.toString()
            is VerificationOrder -> id.toString()
            is Payment -> id.toString()
            is PhysicalDocumentOrder -> id.toString()
            else -> "unknown"
        }
        auditService.log(actor, action, entityName, entityId, mapOf("label" to label))
        return this
    }

    private fun logAction(actorId: Long, action: String, entityName: String, entityId: String, details: Any?) {
        auditService.log(requireUser(actorId), action, entityName, entityId, details)
    }
}
