package com.sivemor.platform.admin

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.common.NotFoundException
import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.CedisRepository
import com.sivemor.platform.domain.ClientCompanyRepository
import com.sivemor.platform.domain.InspectionRepository
import com.sivemor.platform.domain.InspectionResult
import com.sivemor.platform.domain.InspectionStatus
import com.sivemor.platform.domain.OrderUnitRepository
import com.sivemor.platform.domain.PaymentRepository
import com.sivemor.platform.domain.PhysicalDocumentOrderRepository
import com.sivemor.platform.domain.RegionRepository
import com.sivemor.platform.domain.Role
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.domain.VehicleUnitRepository
import com.sivemor.platform.domain.EvaluacionRepository
import com.sivemor.platform.domain.VerificacionRepository
import com.sivemor.platform.domain.VerificacionVeredicto
import com.sivemor.platform.domain.VerificationOrderRepository
import com.sivemor.platform.service.AuditService
import com.sivemor.platform.service.MerCompatibilityService
import com.sivemor.platform.service.PasswordGenerator
import com.sivemor.platform.service.UserCredentialMailer
import com.sivemor.platform.support.TestEntityFactory.client
import com.sivemor.platform.support.TestEntityFactory.checklistQuestion
import com.sivemor.platform.support.TestEntityFactory.checklistSection
import com.sivemor.platform.support.TestEntityFactory.checklistTemplate
import com.sivemor.platform.support.TestEntityFactory.cedis
import com.sivemor.platform.support.TestEntityFactory.inspection
import com.sivemor.platform.support.TestEntityFactory.inspectionAnswer
import com.sivemor.platform.support.TestEntityFactory.inspectionEvidence
import com.sivemor.platform.support.TestEntityFactory.orderUnit
import com.sivemor.platform.support.TestEntityFactory.region
import com.sivemor.platform.support.TestEntityFactory.templateWithSection
import com.sivemor.platform.support.TestEntityFactory.user
import com.sivemor.platform.support.TestEntityFactory.vehicle
import com.sivemor.platform.support.TestEntityFactory.verificationCenter
import com.sivemor.platform.support.TestEntityFactory.verificationOrder
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
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
    @MockK private lateinit var cedisRepository: CedisRepository
    @MockK private lateinit var verificationCenterRepository: com.sivemor.platform.domain.VerificationCenterRepository
    @MockK private lateinit var vehicleUnitRepository: VehicleUnitRepository
    @MockK private lateinit var verificationOrderRepository: VerificationOrderRepository
    @MockK private lateinit var orderUnitRepository: OrderUnitRepository
    @MockK private lateinit var paymentRepository: PaymentRepository
    @MockK private lateinit var physicalDocumentOrderRepository: PhysicalDocumentOrderRepository
    @MockK private lateinit var inspectionRepository: InspectionRepository
    @MockK private lateinit var verificacionRepository: VerificacionRepository
    @MockK private lateinit var evaluacionRepository: EvaluacionRepository
    @MockK private lateinit var passwordEncoder: PasswordEncoder
    @MockK private lateinit var auditService: AuditService
    @MockK private lateinit var passwordGenerator: PasswordGenerator
    @MockK private lateinit var userCredentialMailer: UserCredentialMailer
    @MockK private lateinit var merCompatibilityService: MerCompatibilityService

    private lateinit var adminService: AdminService

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        adminService = AdminService(
            userRepository,
            regionRepository,
            clientCompanyRepository,
            cedisRepository,
            verificationCenterRepository,
            vehicleUnitRepository,
            verificationOrderRepository,
            orderUnitRepository,
            paymentRepository,
            physicalDocumentOrderRepository,
            inspectionRepository,
            verificacionRepository,
            evaluacionRepository,
            passwordEncoder,
            auditService,
            passwordGenerator,
            userCredentialMailer,
            merCompatibilityService
        )
        every { auditService.log(any(), any(), any(), any(), any()) } just Runs
        every { userCredentialMailer.sendNewPassword(any(), any()) } just Runs
        every { verificacionRepository.findAllByArchivedFalseOrderByFechaVerificacionDesc() } returns emptyList()
    }

    @Test
    fun `createUser generates password and emails it`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        every { userRepository.findByUsernameIgnoreCaseAndArchivedFalse("newuser") } returns null
        every { userRepository.findByEmailIgnoreCaseAndArchivedFalse("newuser@example.com") } returns null
        every { userRepository.findById(99L) } returns Optional.of(actor)
        every { passwordGenerator.generate(any()) } returns "Temp1234!"
        every { passwordEncoder.encode("Temp1234!") } returns "encoded-temp"
        every { userRepository.save(any()) } answers { firstArg() }

        val response = adminService.createUser(
            99L,
            UserUpsertRequest("newuser", "newuser@example.com", "New User", Role.ADMIN)
        )

        assertThat(response.username).isEqualTo("newuser")
        verify { userCredentialMailer.sendNewPassword(match { it.username == "newuser" }, "Temp1234!") }
    }

    @Test
    fun `createUser rejects duplicate usernames`() {
        every { userRepository.findByUsernameIgnoreCaseAndArchivedFalse("newuser") } returns user(id = 10L, username = "newuser")

        assertThatThrownBy {
            adminService.createUser(
                99L,
                UserUpsertRequest("newuser", "newuser@example.com", "New User", Role.ADMIN)
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Username already exists")
    }

    @Test
    fun `createUser rejects duplicate emails`() {
        every { userRepository.findByUsernameIgnoreCaseAndArchivedFalse("newuser") } returns null
        every { userRepository.findByEmailIgnoreCaseAndArchivedFalse("newuser@example.com") } returns user(id = 10L, username = "existing")

        assertThatThrownBy {
            adminService.createUser(
                99L,
                UserUpsertRequest("newuser", "newuser@example.com", "New User", Role.ADMIN)
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Email already exists")
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
    fun `listCedis returns only active records ordered by name`() {
        val first = cedis(id = 10L, name = "CEDIS Centro")
        val second = cedis(id = 11L, name = "CEDIS Norte")

        every { cedisRepository.findAllByArchivedFalseOrderByNameAsc() } returns listOf(first, second)

        val result = adminService.listCedis()

        assertThat(result.map { it.name }).containsExactly("CEDIS Centro", "CEDIS Norte")
        assertThat(result.first().email).isEqualTo(first.email)
    }

    @Test
    fun `createClient normalizes contact data and audits new records`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        every { clientCompanyRepository.findByNameIgnoreCaseAndArchivedFalse("Coca-Cola") } returns null
        every { clientCompanyRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById(99L) } returns Optional.of(actor)

        val result = adminService.createClient(
            99L,
            ClientUpsertRequest(
                name = "  Coca-Cola  ",
                businessName = "  Coca-Cola Femsa  ",
                email = "  Clientes@CocaCola.com ",
                phone = "777-450-1100",
                alternatePhone = "(777) 450 2200",
                manager = "  Gestor principal  "
            )
        )

        assertThat(result.name).isEqualTo("Coca-Cola")
        assertThat(result.businessName).isEqualTo("Coca-Cola Femsa")
        assertThat(result.email).isEqualTo("clientes@cocacola.com")
        assertThat(result.phone).isEqualTo("7774501100")
        assertThat(result.alternatePhone).isEqualTo("7774502200")
        assertThat(result.manager).isEqualTo("Gestor principal")
        verify { auditService.log(actor, "CREATE_CLIENT", "ClientCompany", any(), any()) }
    }

    @Test
    fun `getClient rejects archived records`() {
        every { clientCompanyRepository.findById(77L) } returns Optional.of(client(id = 77L, archived = true))

        assertThatThrownBy {
            adminService.getClient(77L)
        }.isInstanceOf(NotFoundException::class.java)
            .hasMessage("Client 77 was not found")
    }

    @Test
    fun `createVerificationCenter normalizes contact data and audits new records`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        val region = region(id = 10L, name = "Centro")
        every { verificationCenterRepository.findByNameIgnoreCaseAndArchivedFalse("Verificentro Centro") } returns null
        every { verificationCenterRepository.findByCenterKeyIgnoreCaseAndArchivedFalse("VER-MOR-002") } returns null
        every { verificationCenterRepository.save(any()) } answers { firstArg() }
        every { regionRepository.findById(10L) } returns Optional.of(region)
        every { userRepository.findById(99L) } returns Optional.of(actor)

        val result = adminService.createVerificationCenter(
            99L,
            VerificationCenterUpsertRequest(
                name = "  Verificentro Centro  ",
                centerKey = " ver-mor-002 ",
                address = "  Av. Plan de Ayala 450  ",
                regionId = 10L,
                manager = "  Ing. Roberto Estrada ",
                email = " Contacto@Verisur-Mor.mx ",
                phone = "777-102-3040",
                alternatePhone = "777 312 4568",
                schedule = "  Lun - Sab: 08:00 a 19:00 "
            )
        )

        assertThat(result.name).isEqualTo("Verificentro Centro")
        assertThat(result.centerKey).isEqualTo("VER-MOR-002")
        assertThat(result.email).isEqualTo("contacto@verisur-mor.mx")
        assertThat(result.phone).isEqualTo("7771023040")
        assertThat(result.alternatePhone).isEqualTo("7773124568")
        assertThat(result.regionName).isEqualTo("Centro")
        verify { auditService.log(actor, "CREATE_VERIFICATION_CENTER", "VerificationCenter", any(), any()) }
    }

    @Test
    fun `getVerificationCenter rejects archived records`() {
        every { verificationCenterRepository.findById(77L) } returns Optional.of(verificationCenter(id = 77L, archived = true))

        assertThatThrownBy {
            adminService.getVerificationCenter(77L)
        }.isInstanceOf(NotFoundException::class.java)
            .hasMessage("Verification center 77 was not found")
    }

    @Test
    fun `createCedis normalizes and audits new records`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        every { cedisRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById(99L) } returns Optional.of(actor)

        val result = adminService.createCedis(
            99L,
            CedisUpsertRequest(
                name = "  CEDIS Norte  ",
                email = "  Norte@Empresa.com ",
                phone = " 7774501122 ",
                alternatePhone = " 7774501100 "
            )
        )

        assertThat(result.name).isEqualTo("CEDIS Norte")
        assertThat(result.email).isEqualTo("norte@empresa.com")
        assertThat(result.phone).isEqualTo("7774501122")
        assertThat(result.alternatePhone).isEqualTo("7774501100")
        verify { auditService.log(actor, "CREATE_CEDIS", "Cedis", any(), any()) }
    }

    @Test
    fun `archiveCedis marks record as archived`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        val target = cedis(id = 33L, name = "CEDIS Sur")

        every { cedisRepository.findById(33L) } returns Optional.of(target)
        every { userRepository.findById(99L) } returns Optional.of(actor)

        adminService.archiveCedis(99L, 33L)

        assertThat(target.archived).isTrue()
        verify {
            auditService.log(
                actor = actor,
                action = "ARCHIVE_CEDIS",
                entityName = "CEDIS",
                entityId = "33",
                details = match<Map<String, String>> { it["name"] == "CEDIS Sur" }
            )
        }
    }

    @Test
    fun `getCedis rejects archived records`() {
        every { cedisRepository.findById(77L) } returns Optional.of(cedis(id = 77L, archived = true))

        assertThatThrownBy {
            adminService.getCedis(77L)
        }.isInstanceOf(NotFoundException::class.java)
            .hasMessage("CEDIS 77 was not found")
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
    fun `generateWebVerificationPdfReport returns a non empty pdf for synced verification`() {
        val template = checklistTemplate(sections = mutableListOf())
        val sectionTitles = listOf(
            "Luces",
            "Llantas",
            "Direccion",
            "Aire Frenos",
            "Motor y emisiones",
            "Otros"
        )
        val sections = sectionTitles.mapIndexed { index, title ->
            checklistSection(
                id = (index + 1).toLong(),
                template = template,
                title = title,
                displayOrder = index + 1,
                questions = mutableListOf()
            ).also { section ->
                val question = checklistQuestion(
                    id = (index + 1).toLong(),
                    section = section,
                    code = "Q_${index + 1}",
                    displayOrder = 1
                ).apply {
                    prompt = "$title pregunta"
                }
                section.questions += question
            }
        }
        template.sections += sections
        val section = template.sections.first()
        val question = section.questions.first()
        val region = region(id = 10L, name = "Centro")
        val client = client(id = 20L, region = region, name = "Cliente Demo")
        val technician = user(id = 30L, role = Role.TECHNICIAN).apply { fullName = "Tecnico Demo" }
        val order = verificationOrder(id = 40L, client = client, region = region, technician = technician)
        val unit = orderUnit(id = 50L, order = order, vehicle = vehicle(id = 60L, client = client, plate = "ABC123"))
        val inspection = inspection(
            id = 70L,
            order = order,
            orderUnit = unit,
            technician = technician,
            template = template,
            status = InspectionStatus.SUBMITTED,
            overallResult = InspectionResult.FAIL
        ).apply {
            submittedAt = Instant.parse("2030-01-05T00:00:00Z")
            answers += inspectionAnswer(id = 71L, inspection = this, question = question, answerValue = AnswerValue.FAIL)
            evidences += inspectionEvidence(id = 72L, inspection = this, section = section)
        }
        val verification = com.sivemor.platform.domain.Verificacion().apply {
            id = 80L
            this.inspection = inspection
            vehicleUnit = unit.vehicleUnit
            verificationOrder = order
            folioVerificacion = "FOLIO-80"
            fechaVerificacion = Instant.parse("2030-01-05T00:00:00Z")
            veredicto = VerificacionVeredicto.REPROBADO
            overallComment = "Comentario de prueba"
        }

        every { verificacionRepository.findById(80L) } returns Optional.of(verification)
        every { inspectionRepository.findDetailedById(70L) } returns inspection
        every { merCompatibilityService.syncSubmittedInspection(inspection) } returns verification
        every { evaluacionRepository.findByVerificacionIdAndArchivedFalse(80L) } returns null

        val pdf = adminService.generateWebVerificationPdfReport(80L)
        val pdfText = Loader.loadPDF(pdf.content).use { document ->
            PDFTextStripper().getText(document)
        }

        assertThat(pdf.filename).contains("ABC123")
        assertThat(pdf.content).isNotEmpty
        assertThat(String(pdf.content.copyOfRange(0, 4))).isEqualTo("%PDF")
        assertThat(pdfText).contains("Luces")
        assertThat(pdfText).contains("Llantas")
        assertThat(pdfText).contains("Direccion")
        assertThat(pdfText).contains("Aire Frenos")
        assertThat(pdfText).contains("Motor y emisiones")
        assertThat(pdfText).contains("Otros")
    }

    @Test
    fun `updateUser rejects archived users`() {
        val archived = user(id = 77L, archived = true)
        every { userRepository.findById(77L) } returns Optional.of(archived)

        assertThatThrownBy {
            adminService.updateUser(
                99L,
                77L,
                UserUpsertRequest("archived", "archived@example.com", "Archived User", Role.ADMIN)
            )
        }.isInstanceOf(NotFoundException::class.java)
            .hasMessage("User 77 was not found")
    }

    @Test
    fun `updateUser rejects duplicate email owned by another user`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        val target = user(id = 77L, username = "target", role = Role.ADMIN).apply {
            email = "target@example.com"
            fullName = "Target User"
        }
        val existing = user(id = 88L, username = "existing", role = Role.ADMIN).apply {
            email = "shared@example.com"
        }
        every { userRepository.findById(77L) } returns Optional.of(target)
        every { userRepository.findByUsernameIgnoreCaseAndArchivedFalse("target") } returns target
        every { userRepository.findByEmailIgnoreCaseAndArchivedFalse("shared@example.com") } returns existing
        every { userRepository.findById(99L) } returns Optional.of(actor)

        assertThatThrownBy {
            adminService.updateUser(
                99L,
                77L,
                UserUpsertRequest("target", "shared@example.com", "Target User", Role.ADMIN)
            )
        }.isInstanceOf(BadRequestException::class.java)
            .hasMessage("Email already exists")
    }

    @Test
    fun `resetUserPassword replaces hash and emails user`() {
        val actor = user(id = 99L, username = "admin", role = Role.ADMIN)
        val target = user(id = 33L, username = "techuser").apply {
            fullName = "Tech User"
            email = "tech@example.com"
        }
        every { userRepository.findById(33L) } returns Optional.of(target)
        every { userRepository.findById(99L) } returns Optional.of(actor)
        every { passwordGenerator.generate(any()) } returns "NewPass123!"
        every { passwordEncoder.encode("NewPass123!") } returns "encoded-reset"

        val response = adminService.resetUserPassword(99L, 33L)

        assertThat(target.passwordHash).isEqualTo("encoded-reset")
        assertThat(response.message).contains("enviada al correo")
        verify { userCredentialMailer.sendNewPassword(target, "NewPass123!") }
        verify { auditService.log(actor, "RESET_USER_PASSWORD", "USER", "33", any()) }
    }
}
