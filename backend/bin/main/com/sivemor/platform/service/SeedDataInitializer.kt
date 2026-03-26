package com.sivemor.platform.service

import com.sivemor.platform.domain.ChecklistQuestion
import com.sivemor.platform.domain.ChecklistSection
import com.sivemor.platform.domain.ChecklistTemplate
import com.sivemor.platform.domain.ChecklistTemplateRepository
import com.sivemor.platform.domain.ClientCompany
import com.sivemor.platform.domain.ClientCompanyRepository
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
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant

@Component
class SeedDataInitializer(
    private val userRepository: UserRepository,
    private val regionRepository: RegionRepository,
    private val clientCompanyRepository: ClientCompanyRepository,
    private val vehicleUnitRepository: VehicleUnitRepository,
    private val verificationOrderRepository: VerificationOrderRepository,
    private val orderUnitRepository: OrderUnitRepository,
    private val checklistTemplateRepository: ChecklistTemplateRepository,
    private val paymentRepository: PaymentRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        if (userRepository.count() > 0L) {
            return
        }

        val northRegion = regionRepository.save(Region().apply { name = "Región Norte" })
        val southRegion = regionRepository.save(Region().apply { name = "Región Sur" })

        val admin = userRepository.save(
            User().apply {
                username = "admin"
                email = "admin@sivemor.local"
                fullName = "Administrador SIVEMOR"
                passwordHash = passwordEncoder.encode("Admin123!")!!
                role = Role.ADMIN
            }
        )

        val technician = userRepository.save(
            User().apply {
                username = "tecnico1"
                email = "tecnico1@sivemor.local"
                fullName = "Técnico Uno"
                passwordHash = passwordEncoder.encode("Tecnico123!")!!
                role = Role.TECHNICIAN
            }
        )

        val clientCompany = clientCompanyRepository.save(
            ClientCompany().apply {
                name = "Transportes Morelos"
                taxId = "TMO260217AA1"
                region = northRegion
            }
        )

        val vehicle = vehicleUnitRepository.save(
            VehicleUnit().apply {
                this.clientCompany = clientCompany
                plate = "MOR-001-A"
                vin = "3ALACXDT3JDJP0010"
                category = VehicleCategory.N2
                brand = "Freightliner"
                model = "M2 106"
            }
        )

        val secondVehicle = vehicleUnitRepository.save(
            VehicleUnit().apply {
                this.clientCompany = clientCompany
                plate = "MOR-002-A"
                vin = "3ALACXDT3JDJP0011"
                category = VehicleCategory.N3
                brand = "Kenworth"
                model = "T680"
            }
        )

        val template = ChecklistTemplate().apply {
            name = "Checklist comercial N2/N3"
            version = 1
            current = true
        }

        val sectionOne = ChecklistSection().apply {
            this.template = template
            title = "Sistema de luces"
            description = "Inspección visual de faros, cuartos y luces de freno."
            displayOrder = 1
        }

        sectionOne.questions += listOf(
            ChecklistQuestion().apply {
                section = sectionOne
                code = "LIGHTS-LOW"
                prompt = "Las luces bajas funcionan correctamente."
                displayOrder = 1
            },
            ChecklistQuestion().apply {
                section = sectionOne
                code = "LIGHTS-BRAKE"
                prompt = "Las luces de freno encienden al accionar el pedal."
                displayOrder = 2
            },
            ChecklistQuestion().apply {
                section = sectionOne
                code = "LIGHTS-TURN"
                prompt = "Las direccionales son visibles y no presentan daño."
                displayOrder = 3
            }
        )

        val sectionTwo = ChecklistSection().apply {
            this.template = template
            title = "Frenos y suspensión"
            description = "Comprobación operativa y visual de frenos y amortiguación."
            displayOrder = 2
        }

        sectionTwo.questions += listOf(
            ChecklistQuestion().apply {
                section = sectionTwo
                code = "BRAKE-PRESSURE"
                prompt = "La presión de aire de frenos se mantiene en rango."
                displayOrder = 1
            },
            ChecklistQuestion().apply {
                section = sectionTwo
                code = "BRAKE-LINES"
                prompt = "No existen fugas visibles en líneas de freno."
                displayOrder = 2
            },
            ChecklistQuestion().apply {
                section = sectionTwo
                code = "SUSPENSION"
                prompt = "La suspensión no presenta daño estructural."
                displayOrder = 3
            }
        )

        template.sections += mutableListOf(sectionOne, sectionTwo)
        checklistTemplateRepository.save(template)

        val order = verificationOrderRepository.save(
            VerificationOrder().apply {
                orderNumber = "PED-2026-0001"
                this.clientCompany = clientCompany
                region = northRegion
                assignedTechnician = technician
                status = VerificationOrderStatus.OPEN
                scheduledAt = Instant.now(clock).plusSeconds(86_400)
                notes = "Pedido inicial generado para pruebas funcionales."
            }
        )

        orderUnitRepository.saveAll(
            listOf(
                OrderUnit().apply {
                    verificationOrder = order
                    vehicleUnit = vehicle
                },
                OrderUnit().apply {
                    verificationOrder = order
                    vehicleUnit = secondVehicle
                }
            )
        )

        paymentRepository.save(
            Payment().apply {
                verificationOrder = order
                amount = BigDecimal("1250.00")
                currency = "MXN"
                status = PaymentStatus.PENDING
                reference = "PAGO-DEMO-0001"
                notes = "Registro de pago inicial para flujo administrativo."
            }
        )

        val secondaryClient = clientCompanyRepository.save(
            ClientCompany().apply {
                name = "Carga del Sur"
                taxId = "CDS260217AA2"
                region = southRegion
            }
        )

        vehicleUnitRepository.save(
            VehicleUnit().apply {
                this.clientCompany = secondaryClient
                plate = "MOR-003-A"
                vin = "1HTMMAAN94H603002"
                category = VehicleCategory.N2
                brand = "International"
                model = "4300"
            }
        )

        val unusedAdmin = admin
        if (unusedAdmin.id == null) {
            throw IllegalStateException("Seed admin user not created")
        }
    }
}
