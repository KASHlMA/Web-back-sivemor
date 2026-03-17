package com.sivemor.platform.admin

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.common.NotFoundException
import com.sivemor.platform.domain.AnswerValue
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
import com.sivemor.platform.domain.Region
import com.sivemor.platform.domain.RegionRepository
import com.sivemor.platform.domain.Role
import com.sivemor.platform.domain.User
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.domain.VehicleCategory
import com.sivemor.platform.domain.VehicleUnit
import com.sivemor.platform.domain.VehicleUnitRepository
import com.sivemor.platform.domain.VerificationOrder
import com.sivemor.platform.domain.VerificationOrderRepository
import com.sivemor.platform.domain.VerificationOrderStatus
import com.sivemor.platform.security.AppUserPrincipal
import com.sivemor.platform.service.AuditService
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
    val active: Boolean = true,
    val password: String? = null
)

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val role: Role,
    val active: Boolean
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
    @field:NotBlank @field:Size(max = 30) val taxId: String,
    @field:NotNull val regionId: Long
)

data class ClientResponse(
    val id: Long,
    val name: String,
    val taxId: String,
    val regionId: Long,
    val regionName: String
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
    @field:DecimalMin("0.00") val amount: BigDecimal,
    @field:NotBlank @field:Size(max = 10) val currency: String = "MXN",
    @field:NotNull val status: PaymentStatus,
    @field:Size(max = 120) val reference: String? = null,
    val notes: String? = null,
    val paidAt: Instant? = null
)

data class PaymentResponse(
    val id: Long,
    val verificationOrderId: Long,
    val orderNumber: String,
    val amount: BigDecimal,
    val currency: String,
    val status: PaymentStatus,
    val reference: String?,
    val notes: String?,
    val paidAt: Instant?
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
@RequestMapping("/api/v1/admin")
class AdminController(
    private val adminService: AdminService
) {
    @GetMapping("/users")
    fun users(): List<UserResponse> = adminService.listUsers()

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    fun createUser(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: UserUpsertRequest
    ): UserResponse = adminService.createUser(principal.id, request)

    @PutMapping("/users/{id}")
    fun updateUser(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: UserUpsertRequest
    ): UserResponse = adminService.updateUser(principal.id, id, request)

    @DeleteMapping("/users/{id}")
    fun deleteUser(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveUser(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/regions")
    fun regions(): List<RegionResponse> = adminService.listRegions()

    @PostMapping("/regions")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRegion(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: RegionUpsertRequest
    ): RegionResponse = adminService.createRegion(principal.id, request)

    @PutMapping("/regions/{id}")
    fun updateRegion(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: RegionUpsertRequest
    ): RegionResponse = adminService.updateRegion(principal.id, id, request)

    @DeleteMapping("/regions/{id}")
    fun deleteRegion(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveRegion(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/clients")
    fun clients(): List<ClientResponse> = adminService.listClients()

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    fun createClient(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: ClientUpsertRequest
    ): ClientResponse = adminService.createClient(principal.id, request)

    @PutMapping("/clients/{id}")
    fun updateClient(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: ClientUpsertRequest
    ): ClientResponse = adminService.updateClient(principal.id, id, request)

    @DeleteMapping("/clients/{id}")
    fun deleteClient(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveClient(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/vehicles")
    fun vehicles(): List<VehicleResponse> = adminService.listVehicles()

    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    fun createVehicle(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: VehicleUpsertRequest
    ): VehicleResponse = adminService.createVehicle(principal.id, request)

    @PutMapping("/vehicles/{id}")
    fun updateVehicle(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: VehicleUpsertRequest
    ): VehicleResponse = adminService.updateVehicle(principal.id, id, request)

    @DeleteMapping("/vehicles/{id}")
    fun deleteVehicle(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveVehicle(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/orders")
    fun orders(): List<OrderResponse> = adminService.listOrders()

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun createOrder(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: OrderUpsertRequest
    ): OrderResponse = adminService.createOrder(principal.id, request)

    @PutMapping("/orders/{id}")
    fun updateOrder(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: OrderUpsertRequest
    ): OrderResponse = adminService.updateOrder(principal.id, id, request)

    @DeleteMapping("/orders/{id}")
    fun deleteOrder(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archiveOrder(principal.id, id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/payments")
    fun payments(): List<PaymentResponse> = adminService.listPayments()

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    fun createPayment(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @Valid @RequestBody request: PaymentUpsertRequest
    ): PaymentResponse = adminService.createPayment(principal.id, request)

    @PutMapping("/payments/{id}")
    fun updatePayment(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PaymentUpsertRequest
    ): PaymentResponse = adminService.updatePayment(principal.id, id, request)

    @DeleteMapping("/payments/{id}")
    fun deletePayment(
        @AuthenticationPrincipal principal: AppUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Unit> {
        adminService.archivePayment(principal.id, id)
        return ResponseEntity.noContent().build()
    }

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

    @GetMapping("/dashboard/failures")
    fun dashboardFailures(): DashboardFailuresResponse = adminService.dashboardFailures()
}

@org.springframework.stereotype.Service
class AdminService(
    private val userRepository: UserRepository,
    private val regionRepository: RegionRepository,
    private val clientCompanyRepository: ClientCompanyRepository,
    private val vehicleUnitRepository: VehicleUnitRepository,
    private val verificationOrderRepository: VerificationOrderRepository,
    private val orderUnitRepository: OrderUnitRepository,
    private val paymentRepository: PaymentRepository,
    private val inspectionRepository: InspectionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val auditService: AuditService
) {
    @Transactional(readOnly = true)
    fun listUsers(): List<UserResponse> = userRepository.findAllByArchivedFalseOrderByFullNameAsc().map { it.toResponse() }

    @Transactional
    fun createUser(actorId: Long, request: UserUpsertRequest): UserResponse {
        if (request.password.isNullOrBlank()) {
            throw BadRequestException("Password is required for new users")
        }

        if (userRepository.findByUsernameIgnoreCaseAndArchivedFalse(request.username.trim()) != null) {
            throw BadRequestException("Username already exists")
        }

        val user = User().apply {
            username = request.username.trim()
            email = request.email.trim().lowercase()
            fullName = request.fullName.trim()
            role = request.role
            active = request.active
            passwordHash = passwordEncoder.encode(request.password!!) ?: throw IllegalStateException("Password encoder returned null")
        }

        return userRepository.save(user).alsoSaved(actorId, "CREATE_USER", user.fullName).toResponse()
    }

    @Transactional
    fun updateUser(actorId: Long, id: Long, request: UserUpsertRequest): UserResponse {
        val user = requireUser(id)
        user.username = request.username.trim()
        user.email = request.email.trim().lowercase()
        user.fullName = request.fullName.trim()
        user.role = request.role
        user.active = request.active
        if (!request.password.isNullOrBlank()) {
            user.passwordHash = passwordEncoder.encode(request.password!!) ?: throw IllegalStateException("Password encoder returned null")
        }
        return user.alsoSaved(actorId, "UPDATE_USER", user.fullName).toResponse()
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

    @Transactional
    fun createClient(actorId: Long, request: ClientUpsertRequest): ClientResponse =
        clientCompanyRepository.save(
            ClientCompany().apply {
                name = request.name.trim()
                taxId = request.taxId.trim().uppercase()
                region = requireRegion(request.regionId)
            }
        ).alsoSaved(actorId, "CREATE_CLIENT", request.name).toResponse()

    @Transactional
    fun updateClient(actorId: Long, id: Long, request: ClientUpsertRequest): ClientResponse =
        requireClient(id).apply {
            name = request.name.trim()
            taxId = request.taxId.trim().uppercase()
            region = requireRegion(request.regionId)
        }.alsoSaved(actorId, "UPDATE_CLIENT", request.name).toResponse()

    @Transactional
    fun archiveClient(actorId: Long, id: Long) {
        val client = requireClient(id)
        client.archived = true
        logAction(actorId, "ARCHIVE_CLIENT", "CLIENT_COMPANY", client.id.toString(), mapOf("name" to client.name))
    }

    @Transactional(readOnly = true)
    fun listVehicles(): List<VehicleResponse> = vehicleUnitRepository.findAllByArchivedFalseOrderByPlateAsc().map { it.toResponse() }

    @Transactional
    fun createVehicle(actorId: Long, request: VehicleUpsertRequest): VehicleResponse =
        vehicleUnitRepository.save(
            VehicleUnit().apply {
                clientCompany = requireClient(request.clientCompanyId)
                plate = request.plate.trim().uppercase()
                vin = request.vin.trim().uppercase()
                category = request.category
                brand = request.brand.trim()
                model = request.model.trim()
            }
        ).alsoSaved(actorId, "CREATE_VEHICLE", request.plate).toResponse()

    @Transactional
    fun updateVehicle(actorId: Long, id: Long, request: VehicleUpsertRequest): VehicleResponse =
        requireVehicle(id).apply {
            clientCompany = requireClient(request.clientCompanyId)
            plate = request.plate.trim().uppercase()
            vin = request.vin.trim().uppercase()
            category = request.category
            brand = request.brand.trim()
            model = request.model.trim()
        }.alsoSaved(actorId, "UPDATE_VEHICLE", request.plate).toResponse()

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

    @Transactional
    fun createPayment(actorId: Long, request: PaymentUpsertRequest): PaymentResponse =
        paymentRepository.save(
            Payment().apply {
                verificationOrder = requireOrder(request.verificationOrderId)
                amount = request.amount
                currency = request.currency.uppercase()
                status = request.status
                reference = request.reference?.trim()
                notes = request.notes?.trim()
                paidAt = request.paidAt
            }
        ).alsoSaved(actorId, "CREATE_PAYMENT", request.reference ?: "payment").toResponse()

    @Transactional
    fun updatePayment(actorId: Long, id: Long, request: PaymentUpsertRequest): PaymentResponse =
        requirePayment(id).apply {
            verificationOrder = requireOrder(request.verificationOrderId)
            amount = request.amount
            currency = request.currency.uppercase()
            status = request.status
            reference = request.reference?.trim()
            notes = request.notes?.trim()
            paidAt = request.paidAt
        }.alsoSaved(actorId, "UPDATE_PAYMENT", request.reference ?: "payment").toResponse()

    @Transactional
    fun archivePayment(actorId: Long, id: Long) {
        val payment = requirePayment(id)
        payment.archived = true
        logAction(actorId, "ARCHIVE_PAYMENT", "PAYMENT", payment.id.toString(), mapOf("reference" to payment.reference))
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
    ): List<ReportSummaryResponse> = inspectionRepository
        .findAllByStatusAndArchivedFalseOrderBySubmittedAtDesc(InspectionStatus.SUBMITTED)
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
        .map { it.toReportSummary() }

    @Transactional(readOnly = true)
    fun dashboardFailures(): DashboardFailuresResponse {
        val submitted = inspectionRepository.findAllByStatusAndArchivedFalseOrderBySubmittedAtDesc(InspectionStatus.SUBMITTED)
        val failed = submitted.filter { it.overallResult == InspectionResult.FAIL }
        val failuresByRegion = failed
            .groupBy { it.verificationOrder.region.name }
            .map { FailureBucketResponse(label = it.key, count = it.value.size.toLong()) }
            .sortedByDescending { it.count }

        return DashboardFailuresResponse(
            totalSubmitted = submitted.size.toLong(),
            totalFailed = failed.size.toLong(),
            unitsWithProblems = failed.mapNotNull { it.orderUnit.vehicleUnit.id }.distinct().size.toLong(),
            failuresByRegion = failuresByRegion,
            recentFailures = failed.take(10).map { it.toReportSummary() }
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
        taxId = taxId,
        regionId = region.id ?: 0L,
        regionName = region.name
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
        amount = amount,
        currency = currency,
        status = status,
        reference = reference,
        notes = notes,
        paidAt = paidAt
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

    private fun <T : Any> T.alsoSaved(actorId: Long, action: String, label: String): T {
        val actor = requireUser(actorId)
        val entityName = this::class.simpleName ?: "ENTITY"
        val entityId = when (this) {
            is User -> id.toString()
            is Region -> id.toString()
            is ClientCompany -> id.toString()
            is VehicleUnit -> id.toString()
            is VerificationOrder -> id.toString()
            is Payment -> id.toString()
            else -> "unknown"
        }
        auditService.log(actor, action, entityName, entityId, mapOf("label" to label))
        return this
    }

    private fun logAction(actorId: Long, action: String, entityName: String, entityId: String, details: Any?) {
        auditService.log(requireUser(actorId), action, entityName, entityId, details)
    }
}
