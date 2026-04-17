package com.sivemor.platform.service

import com.sivemor.platform.domain.ChecklistQuestion
import com.sivemor.platform.domain.ChecklistSection
import com.sivemor.platform.domain.ChecklistTemplate
import com.sivemor.platform.domain.ChecklistTemplateRepository
import com.sivemor.platform.domain.ClientCompany
import com.sivemor.platform.domain.ClientCompanyRepository
import com.sivemor.platform.domain.Payment
import com.sivemor.platform.domain.PaymentRepository
import com.sivemor.platform.domain.PaymentStatus
import com.sivemor.platform.domain.PaymentType
import com.sivemor.platform.domain.Region
import com.sivemor.platform.domain.RegionRepository
import com.sivemor.platform.domain.Role
import com.sivemor.platform.domain.User
import com.sivemor.platform.domain.UserRepository
import com.sivemor.platform.domain.VerificationCenter
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
    private val verificationCenterRepository: com.sivemor.platform.domain.VerificationCenterRepository,
    private val verificationOrderRepository: VerificationOrderRepository,
    private val checklistTemplateRepository: ChecklistTemplateRepository,
    private val paymentRepository: PaymentRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        val northRegion = regionRepository.findAllByArchivedFalseOrderByNameAsc()
            .firstOrNull { it.name == "Region Norte" }
            ?: regionRepository.save(Region().apply { name = "Region Norte" })
        val southRegion = regionRepository.findAllByArchivedFalseOrderByNameAsc()
            .firstOrNull { it.name == "Region Sur" }
            ?: regionRepository.save(Region().apply { name = "Region Sur" })

        val admin = userRepository.findByUsernameIgnoreCaseAndArchivedFalse("admin")
            ?: userRepository.save(
                User().apply {
                    username = "admin"
                    email = "admin@sivemor.local"
                    fullName = "Administrador SIVEMOR"
                    passwordHash = passwordEncoder.encode("Admin123!")!!
                    role = Role.ADMIN
                }
            )

        val technician = userRepository.findByUsernameIgnoreCaseAndArchivedFalse("tecnico1")
            ?: userRepository.save(
                User().apply {
                    username = "tecnico1"
                    email = "tecnico1@sivemor.local"
                    fullName = "Tecnico Uno"
                    passwordHash = passwordEncoder.encode("Tecnico123!")!!
                    role = Role.TECHNICIAN
                }
            )

        val clientCompany = clientCompanyRepository.findAllByArchivedFalseOrderByNameAsc()
            .firstOrNull { it.name == "Transportes Morelos" }
            ?: clientCompanyRepository.save(
                ClientCompany().apply {
                    name = "Transportes Morelos"
                    businessName = "Transportes Morelos SA de CV"
                    email = "transportes.morelos@sivemor.local"
                    phone = "7774501101"
                    alternatePhone = "7774501102"
                    manager = "Gestor Morelos"
                    taxId = "TMO260217AA1"
                    region = northRegion
                }
            )

        clientCompanyRepository.findAllByArchivedFalseOrderByNameAsc()
            .firstOrNull { it.name == "Carga del Sur" }
            ?: clientCompanyRepository.save(
                ClientCompany().apply {
                    name = "Carga del Sur"
                    businessName = "Carga del Sur SA de CV"
                    email = "carga.sur@sivemor.local"
                    phone = "7774502201"
                    alternatePhone = "7774502202"
                    manager = "Gestor Sur"
                    taxId = "CDS260217AA2"
                    region = southRegion
                }
            )

        if (checklistTemplateRepository.count() == 0L) {
            val template = ChecklistTemplate().apply {
                name = "Checklist comercial N2/N3"
                version = 1
                current = true
            }

            val sectionOne = ChecklistSection().apply {
                this.template = template
                title = "Sistema de luces"
                description = "Inspeccion visual de faros, cuartos y luces de freno."
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
                    prompt = "Las direccionales son visibles y no presentan dano."
                    displayOrder = 3
                }
            )

            val sectionTwo = ChecklistSection().apply {
                this.template = template
                title = "Frenos y suspension"
                description = "Comprobacion operativa y visual de frenos y amortiguacion."
                displayOrder = 2
            }

            sectionTwo.questions += listOf(
                ChecklistQuestion().apply {
                    section = sectionTwo
                    code = "BRAKE-PRESSURE"
                    prompt = "La presion de aire de frenos se mantiene en rango."
                    displayOrder = 1
                },
                ChecklistQuestion().apply {
                    section = sectionTwo
                    code = "BRAKE-LINES"
                    prompt = "No existen fugas visibles en lineas de freno."
                    displayOrder = 2
                },
                ChecklistQuestion().apply {
                    section = sectionTwo
                    code = "SUSPENSION"
                    prompt = "La suspension no presenta dano estructural."
                    displayOrder = 3
                }
            )

            template.sections += mutableListOf(sectionOne, sectionTwo)
            checklistTemplateRepository.save(template)
        }

        val order = verificationOrderRepository.findAllByArchivedFalseOrderByScheduledAtDesc()
            .firstOrNull { it.orderNumber == "PED-2026-0001" }
            ?: verificationOrderRepository.save(
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

        if (paymentRepository.count() == 0L) {
            paymentRepository.save(
                Payment().apply {
                    verificationOrder = order
                    paymentType = PaymentType.CARD
                    amount = BigDecimal("1250.00")
                    status = PaymentStatus.PENDING
                    depositAccount = "Cuenta principal demo"
                    invoiceNumber = "FAC-DEMO-0001"
                }
            )
        }

        if (verificationCenterRepository.findAllByArchivedFalseOrderByNameAsc().none { it.centerKey == "VER-MOR-001" }) {
            verificationCenterRepository.save(
                VerificationCenter().apply {
                    name = "Verificentro Centro"
                    centerKey = "VER-MOR-001"
                    address = "Av. Plan de Ayala 450, Cuernavaca"
                    region = northRegion
                    manager = "Ing. Roberto Estrada"
                    email = "contacto@verisur-mor.mx"
                    phone = "7771023040"
                    alternatePhone = "7773124568"
                    schedule = "Lun - Sab: 08:00 a 19:00"
                }
            )
        }
        if (verificationCenterRepository.findAllByArchivedFalseOrderByNameAsc().none { it.centerKey == "VER-MOR-002" }) {
            verificationCenterRepository.save(
                VerificationCenter().apply {
                    name = "Eco Morelos"
                    centerKey = "VER-MOR-002"
                    address = "Av. Universidad 1200, Cuernavaca"
                    region = southRegion
                    manager = "Lic. Andrea Solis"
                    email = "admin@ecomorelos.mx"
                    phone = "7351238890"
                    alternatePhone = "7351238891"
                    schedule = "Lun - Vie: 08:00 a 18:00"
                }
            )
        }

        if (admin.id == null) {
            throw IllegalStateException("Seed admin user not created")
        }
    }
}
