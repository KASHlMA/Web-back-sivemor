package com.sivemor.platform.admin

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.common.NotFoundException
import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.ClientCompanyRepository
import com.sivemor.platform.domain.InspectionRepository
import com.sivemor.platform.domain.InspectionResult
import com.sivemor.platform.domain.InspectionStatus
import com.sivemor.platform.domain.OrderUnitRepository
import com.sivemor.platform.domain.PaymentRepository
import com.sivemor.platform.domain.RegionRepository
import com.sivemor.platform.domain.Role
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.domain.VehicleUnitRepository
import com.sivemor.platform.domain.VerificationOrderRepository
import com.sivemor.platform.service.AuditService
import com.sivemor.platform.support.TestEntityFactory.client
import com.sivemor.platform.support.TestEntityFactory.inspection
import com.sivemor.platform.support.TestEntityFactory.inspectionAnswer
import com.sivemor.platform.support.TestEntityFactory.inspectionEvidence
import com.sivemor.platform.support.TestEntityFactory.orderUnit
import com.sivemor.platform.support.TestEntityFactory.region
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
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.Optional

class AdminServiceTest {
    @MockK private lateinit var userRepository: UserRepository
    @MockK private lateinit var regionRepository: RegionRepository
    @MockK private lateinit var clientCompanyRepository: ClientCompanyRepository
    @MockK private lateinit var vehicleUnitRepository: VehicleUnitRepository
    @MockK private lateinit var verificationOrderRepository: VerificationOrderRepository
    @MockK private lateinit var orderUnitRepository: OrderUnitRepository
    @MockK private lateinit var paymentRepository: PaymentRepository
    @MockK private lateinit var inspectionRepository: InspectionRepository
    @MockK private lateinit var passwordEncoder: PasswordEncoder
    @MockK private lateinit var auditService: AuditService

    private lateinit var adminService: AdminService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        adminService = AdminService(
            userRepository,
            regionRepository,
            clientCompanyRepository,
            vehicleUnitRepository,
            verificationOrderRepository,
            orderUnitRepository,
            paymentRepository,
            inspectionRepository,
            passwordEncoder,
            auditService
        )
        every { auditService.log(any(), any(), any(), any(), any()) } just Runs
    }

    @Test
    fun `createUser requires a password for new users`() {
        assertThatThrownBy {
            adminService.createUser(
                99L,
                UserUpsertRequest("newuser", "newuser@example.com", "New User", Role.ADMIN, password = null)
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Password is required for new users")
    }

    @Test
    fun `createUser rejects duplicate usernames`() {
        every { userRepository.findByUsernameIgnoreCaseAndArchivedFalse("newuser") } returns user(id = 10L, username = "newuser")

        assertThatThrownBy {
            adminService.createUser(
                99L,
                UserUpsertRequest("newuser", "newuser@example.com", "New User", Role.ADMIN, password = "secret")
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Username already exists")
    }

    @Test
    fun `createOrder rejects vehicles that belong to another client`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        val technician = user(id = 22L, username = "tech", role = Role.TECHNICIAN)
        val region = region(id = 3L)
        val orderClient = client(id = 4L, region = region)
        val differentClient = client(id = 5L, region = region)
        val wrongVehicle = vehicle(id = 6L, client = differentClient)

        every { userRepository.findById(22L) } returns Optional.of(technician)
        every { clientCompanyRepository.findById(4L) } returns Optional.of(orderClient)
        every { regionRepository.findById(3L) } returns Optional.of(region)
        every { verificationOrderRepository.save(any()) } answers { firstArg() }
        every { vehicleUnitRepository.findById(6L) } returns Optional.of(wrongVehicle)
        every { userRepository.findById(99L) } returns Optional.of(actor)

        assertThatThrownBy {
            adminService.createOrder(
                99L,
                OrderUpsertRequest(
                    orderNumber = "order-1",
                    clientCompanyId = 4L,
                    regionId = 3L,
                    assignedTechnicianId = 22L,
                    unitIds = listOf(6L),
                    scheduledAt = Instant.parse("2030-01-02T00:00:00Z")
                )
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Vehicle 6 does not belong to client 4")
    }

    @Test
    fun `createOrder deduplicates unit ids before attaching units`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        val technician = user(id = 22L, username = "tech", role = Role.TECHNICIAN)
        val region = region(id = 3L)
        val client = client(id = 4L, region = region)
        val matchingVehicle = vehicle(id = 6L, client = client)

        every { userRepository.findById(22L) } returns Optional.of(technician)
        every { clientCompanyRepository.findById(4L) } returns Optional.of(client)
        every { regionRepository.findById(3L) } returns Optional.of(region)
        every { verificationOrderRepository.save(any()) } answers { firstArg() }
        every { vehicleUnitRepository.findById(6L) } returns Optional.of(matchingVehicle)
        every { userRepository.findById(99L) } returns Optional.of(actor)

        val response = adminService.createOrder(
            99L,
            OrderUpsertRequest(
                orderNumber = "order-1",
                clientCompanyId = 4L,
                regionId = 3L,
                assignedTechnicianId = 22L,
                unitIds = listOf(6L, 6L),
                scheduledAt = Instant.parse("2030-01-02T00:00:00Z")
            )
        )

        assertThat(response.orderNumber).isEqualTo("ORDER-1")
        assertThat(response.units).hasSize(1)
        verify { auditService.log(actor, "CREATE_ORDER", "VerificationOrder", any(), any()) }
    }

    @Test
    fun `archiveUser deactivates and archives the user`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        val target = user(id = 33L, username = "archivable", role = Role.TECHNICIAN)

        every { userRepository.findById(33L) } returns Optional.of(target)
        every { userRepository.findById(99L) } returns Optional.of(actor)

        adminService.archiveUser(99L, 33L)

        assertThat(target.active).isFalse()
        assertThat(target.archived).isTrue()
        verify {
            auditService.log(
                actor = actor,
                action = "ARCHIVE_USER",
                entityName = "USER",
                entityId = "33",
                details = match<Map<String, String>> { it["username"] == "archivable" }
            )
        }
    }

    @Test
    fun `listReports filters by failure flag and requested criteria`() {
        val template = templateWithSection()
        val question = template.sections.first().questions.first()
        val northRegion = region(id = 10L, name = "North")
        val southRegion = region(id = 11L, name = "South")
        val northClient = client(id = 20L, region = northRegion)
        val southClient = client(id = 21L, region = southRegion)
        val northTechnician = user(id = 30L, role = Role.TECHNICIAN)
        val southTechnician = user(id = 31L, role = Role.TECHNICIAN)
        val northOrder = verificationOrder(id = 40L, client = northClient, region = northRegion, technician = northTechnician)
        val southOrder = verificationOrder(id = 41L, client = southClient, region = southRegion, technician = southTechnician)
        val northOrderUnit = orderUnit(id = 50L, order = northOrder, vehicle = vehicle(id = 60L, client = northClient))
        val southOrderUnit = orderUnit(id = 51L, order = southOrder, vehicle = vehicle(id = 61L, client = southClient))

        val failedInspection = inspection(
            id = 70L,
            order = northOrder,
            orderUnit = northOrderUnit,
            technician = northTechnician,
            template = template,
            status = InspectionStatus.SUBMITTED,
            overallResult = InspectionResult.FAIL
        ).apply {
            submittedAt = Instant.parse("2030-01-05T00:00:00Z")
            answers += inspectionAnswer(id = 71L, inspection = this, question = question, answerValue = AnswerValue.FAIL)
            evidences += inspectionEvidence(id = 72L, inspection = this, section = template.sections.first())
        }

        val passedInspection = inspection(
            id = 80L,
            order = southOrder,
            orderUnit = southOrderUnit,
            technician = southTechnician,
            template = template,
            status = InspectionStatus.SUBMITTED,
            overallResult = InspectionResult.PASS
        ).apply {
            submittedAt = Instant.parse("2030-01-06T00:00:00Z")
        }

        every {
            inspectionRepository.findAllByStatusAndArchivedFalseOrderBySubmittedAtDesc(InspectionStatus.SUBMITTED)
        } returns listOf(passedInspection, failedInspection)

        val failures = adminService.listReports(
            companyId = 20L,
            regionId = 10L,
            orderId = 40L,
            technicianId = 30L,
            vehicleId = 60L,
            from = Instant.parse("2030-01-04T00:00:00Z"),
            to = Instant.parse("2030-01-06T00:00:00Z"),
            onlyFailures = true
        )

        assertThat(failures).singleElement().satisfies({ report ->
            assertThat(report.inspectionId).isEqualTo(70L)
            assertThat(report.failureCount).isEqualTo(1)
            assertThat(report.evidenceCount).isEqualTo(1)
        })
    }

    @Test
    fun `dashboardFailures aggregates counts by region and recent failures`() {
        val template = templateWithSection()
        val question = template.sections.first().questions.first()
        val northRegion = region(id = 10L, name = "North")
        val southRegion = region(id = 11L, name = "South")
        val northClient = client(id = 20L, region = northRegion)
        val southClient = client(id = 21L, region = southRegion)
        val northTech = user(id = 30L, role = Role.TECHNICIAN)
        val southTech = user(id = 31L, role = Role.TECHNICIAN)

        val failedNorthOrder = verificationOrder(id = 40L, client = northClient, region = northRegion, technician = northTech)
        val failedSouthOrder = verificationOrder(id = 41L, client = southClient, region = southRegion, technician = southTech)
        val passedOrder = verificationOrder(id = 42L, client = northClient, region = northRegion, technician = northTech)

        val failedNorth = inspection(
            id = 100L,
            order = failedNorthOrder,
            orderUnit = orderUnit(id = 50L, order = failedNorthOrder, vehicle = vehicle(id = 60L, client = northClient, plate = "NORTH-1")),
            technician = northTech,
            template = template,
            status = InspectionStatus.SUBMITTED,
            overallResult = InspectionResult.FAIL
        ).apply {
            submittedAt = Instant.parse("2030-01-05T00:00:00Z")
            answers += inspectionAnswer(id = 101L, inspection = this, question = question, answerValue = AnswerValue.FAIL)
        }

        val failedSouth = inspection(
            id = 110L,
            order = failedSouthOrder,
            orderUnit = orderUnit(id = 51L, order = failedSouthOrder, vehicle = vehicle(id = 61L, client = southClient, plate = "SOUTH-1")),
            technician = southTech,
            template = template,
            status = InspectionStatus.SUBMITTED,
            overallResult = InspectionResult.FAIL
        ).apply {
            submittedAt = Instant.parse("2030-01-06T00:00:00Z")
            answers += inspectionAnswer(id = 111L, inspection = this, question = question, answerValue = AnswerValue.FAIL)
        }

        val passed = inspection(
            id = 120L,
            order = passedOrder,
            orderUnit = orderUnit(id = 52L, order = passedOrder, vehicle = vehicle(id = 62L, client = northClient, plate = "NORTH-2")),
            technician = northTech,
            template = template,
            status = InspectionStatus.SUBMITTED,
            overallResult = InspectionResult.PASS
        ).apply {
            submittedAt = Instant.parse("2030-01-04T00:00:00Z")
        }

        every {
            inspectionRepository.findAllByStatusAndArchivedFalseOrderBySubmittedAtDesc(InspectionStatus.SUBMITTED)
        } returns listOf(failedSouth, failedNorth, passed)

        val dashboard = adminService.dashboardFailures()

        assertThat(dashboard.totalSubmitted).isEqualTo(3)
        assertThat(dashboard.totalFailed).isEqualTo(2)
        assertThat(dashboard.unitsWithProblems).isEqualTo(2)
        assertThat(dashboard.failuresByRegion).extracting(FailureBucketResponse::label, FailureBucketResponse::count)
            .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("South", 1L), org.assertj.core.groups.Tuple.tuple("North", 1L))
        assertThat(dashboard.recentFailures.map { it.inspectionId })
            .containsExactly(110L, 100L)
    }

    @Test
    fun `updateUser rejects archived users`() {
        val archived = user(id = 77L, archived = true)
        every { userRepository.findById(77L) } returns Optional.of(archived)

        assertThatThrownBy {
            adminService.updateUser(
                99L,
                77L,
                UserUpsertRequest("archived", "archived@example.com", "Archived User", Role.ADMIN, password = "new-password")
            )
        }.isInstanceOf(NotFoundException::class.java)
            .hasMessage("User 77 was not found")
    }
}
