package com.sivemor.platform.support

import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.Cedis
import com.sivemor.platform.domain.ChecklistQuestion
import com.sivemor.platform.domain.ChecklistSection
import com.sivemor.platform.domain.ChecklistTemplate
import com.sivemor.platform.domain.ClientCompany
import com.sivemor.platform.domain.Inspection
import com.sivemor.platform.domain.InspectionAnswer
import com.sivemor.platform.domain.InspectionEvidence
import com.sivemor.platform.domain.InspectionResult
import com.sivemor.platform.domain.InspectionSectionNote
import com.sivemor.platform.domain.InspectionStatus
import com.sivemor.platform.domain.OrderUnit
import com.sivemor.platform.domain.Payment
import com.sivemor.platform.domain.PaymentStatus
import com.sivemor.platform.domain.RefreshToken
import com.sivemor.platform.domain.Region
import com.sivemor.platform.domain.Role
import com.sivemor.platform.domain.User
import com.sivemor.platform.domain.VehicleCategory
import com.sivemor.platform.domain.VehicleUnit
import com.sivemor.platform.domain.VerificationOrder
import com.sivemor.platform.domain.VerificationCenter
import com.sivemor.platform.domain.VerificationOrderStatus
import java.math.BigDecimal
import java.time.Instant

object TestEntityFactory {
    fun user(
        id: Long = 1L,
        username: String = "user$id",
        role: Role = Role.ADMIN,
        active: Boolean = true,
        archived: Boolean = false,
        passwordHash: String = "encoded-password"
    ) = User().apply {
        this.id = id
        this.username = username
        email = "$username@example.com"
        fullName = "User $id"
        this.role = role
        this.active = active
        this.archived = archived
        this.passwordHash = passwordHash
    }

    fun refreshToken(
        id: Long = 1L,
        user: User = user(),
        tokenHash: String = "hash",
        expiresAt: Instant = Instant.parse("2030-01-01T00:00:00Z"),
        revokedAt: Instant? = null
    ) = RefreshToken().apply {
        this.id = id
        this.user = user
        this.tokenHash = tokenHash
        this.expiresAt = expiresAt
        this.revokedAt = revokedAt
    }

    fun region(
        id: Long = 1L,
        name: String = "Region $id",
        archived: Boolean = false
    ) = Region().apply {
        this.id = id
        this.name = name
        this.archived = archived
    }

    fun client(
        id: Long = 1L,
        name: String = "Client $id",
        region: Region = region(),
        archived: Boolean = false
    ) = ClientCompany().apply {
        this.id = id
        this.name = name
        businessName = "$name SA de CV"
        email = "client$id@example.com"
        phone = "77745011$id"
        alternatePhone = "77745022$id"
        manager = "Manager $id"
        taxId = "RFC$id"
        this.region = region
        this.archived = archived
    }

    fun cedis(
        id: Long = 1L,
        name: String = "CEDIS $id",
        archived: Boolean = false
    ) = Cedis().apply {
        this.id = id
        this.name = name
        email = "cedis$id@example.com"
        phone = "77745011$id"
        alternatePhone = "77745022$id"
        this.archived = archived
    }

    fun verificationCenter(
        id: Long = 1L,
        name: String = "Verificentro $id",
        region: Region = region(),
        archived: Boolean = false
    ) = VerificationCenter().apply {
        this.id = id
        this.name = name
        centerKey = "VER-MOR-00$id"
        address = "Direccion $id"
        this.region = region
        manager = "Responsable $id"
        email = "verification$id@example.com"
        phone = "77720011$id"
        alternatePhone = "77720022$id"
        schedule = "Lun - Vie 08:00 - 18:00"
        this.archived = archived
    }

    fun vehicle(
        id: Long = 1L,
        client: ClientCompany = client(),
        plate: String = "ABC$id",
        archived: Boolean = false
    ) = VehicleUnit().apply {
        this.id = id
        clientCompany = client
        this.plate = plate
        vin = "VIN$id"
        category = VehicleCategory.N2
        brand = "Brand"
        model = "Model"
        this.archived = archived
    }

    fun verificationOrder(
        id: Long = 1L,
        client: ClientCompany = client(),
        region: Region = client.region ?: region(),
        technician: User = user(id = 2L, role = Role.TECHNICIAN),
        status: VerificationOrderStatus = VerificationOrderStatus.OPEN
    ) = VerificationOrder().apply {
        this.id = id
        orderNumber = "ORD-$id"
        clientCompany = client
        this.region = region
        assignedTechnician = technician
        this.status = status
        scheduledAt = Instant.parse("2030-01-01T00:00:00Z")
    }

    fun orderUnit(
        id: Long = 1L,
        order: VerificationOrder = verificationOrder(),
        vehicle: VehicleUnit = vehicle(client = order.clientCompany)
    ) = OrderUnit().apply {
        this.id = id
        verificationOrder = order
        vehicleUnit = vehicle
    }

    fun checklistTemplate(
        id: Long = 1L,
        name: String = "Current Template",
        current: Boolean = true,
        sections: MutableList<ChecklistSection> = mutableListOf()
    ) = ChecklistTemplate().apply {
        this.id = id
        this.name = name
        version = 1
        this.current = current
        this.sections = sections
    }

    fun checklistSection(
        id: Long = 1L,
        template: ChecklistTemplate,
        title: String = "Section $id",
        displayOrder: Int = id.toInt(),
        questions: MutableList<ChecklistQuestion> = mutableListOf()
    ) = ChecklistSection().apply {
        this.id = id
        this.template = template
        this.title = title
        description = "$title description"
        this.displayOrder = displayOrder
        this.questions = questions
    }

    fun checklistQuestion(
        id: Long = 1L,
        section: ChecklistSection,
        code: String = "QUESTION-$id",
        required: Boolean = true,
        displayOrder: Int = id.toInt()
    ) = ChecklistQuestion().apply {
        this.id = id
        this.section = section
        this.code = code
        prompt = "Prompt for $code"
        this.required = required
        this.displayOrder = displayOrder
    }

    fun inspection(
        id: Long = 1L,
        order: VerificationOrder = verificationOrder(),
        orderUnit: OrderUnit = orderUnit(order = order),
        technician: User = order.assignedTechnician,
        template: ChecklistTemplate = templateWithSection(),
        status: InspectionStatus = InspectionStatus.DRAFT,
        overallResult: InspectionResult? = null
    ) = Inspection().apply {
        this.id = id
        verificationOrder = order
        this.orderUnit = orderUnit
        this.technician = technician
        this.template = template
        this.status = status
        this.overallResult = overallResult
        startedAt = Instant.parse("2030-01-01T00:00:00Z")
    }

    fun inspectionAnswer(
        id: Long = 1L,
        inspection: Inspection,
        question: ChecklistQuestion,
        answerValue: AnswerValue = AnswerValue.PASS,
        comment: String? = null
    ) = InspectionAnswer().apply {
        this.id = id
        this.inspection = inspection
        this.question = question
        this.answerValue = answerValue
        this.comment = comment
    }

    fun inspectionNote(
        id: Long = 1L,
        inspection: Inspection,
        section: ChecklistSection,
        comment: String = "Note"
    ) = InspectionSectionNote().apply {
        this.id = id
        this.inspection = inspection
        this.section = section
        this.comment = comment
    }

    fun inspectionEvidence(
        id: Long = 1L,
        inspection: Inspection,
        section: ChecklistSection? = null,
        filename: String = "evidence.jpg",
        capturedAt: Instant = Instant.parse("2030-01-01T00:00:00Z")
    ) = InspectionEvidence().apply {
        this.id = id
        this.inspection = inspection
        this.section = section
        this.filename = filename
        mimeType = "image/jpeg"
        checksum = "checksum-$id"
        this.capturedAt = capturedAt
        content = "bytes-$id".encodeToByteArray()
    }

    fun payment(
        id: Long = 1L,
        order: VerificationOrder = verificationOrder(),
        status: PaymentStatus = PaymentStatus.PENDING
    ) = Payment().apply {
        this.id = id
        verificationOrder = order
        amount = BigDecimal("1250.00")
        currency = "MXN"
        this.status = status
        reference = "REF-$id"
    }

    fun templateWithSection(): ChecklistTemplate {
        val template = checklistTemplate(sections = mutableListOf())
        val section = checklistSection(template = template, questions = mutableListOf())
        val questionOne = checklistQuestion(id = 1L, section = section, code = "LIGHTS-LOW")
        val questionTwo = checklistQuestion(id = 2L, section = section, code = "LIGHTS-BRAKE")
        section.questions += mutableListOf(questionOne, questionTwo)
        template.sections += section
        return template
    }
}
