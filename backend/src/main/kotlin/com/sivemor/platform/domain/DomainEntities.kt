package com.sivemor.platform.domain

import jakarta.persistence.Basic
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Lob
import java.math.BigDecimal
import java.time.Instant

enum class Role {
    ADMIN,
    TECHNICIAN
}

enum class VehicleCategory {
    N2,
    N3
}

enum class VerificationOrderStatus {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class InspectionStatus {
    DRAFT,
    PAUSED,
    SUBMITTED
}

enum class InspectionResult {
    PASS,
    FAIL
}

enum class AnswerValue {
    PASS,
    FAIL,
    NA
}

enum class PaymentStatus {
    PENDING,
    PAID,
    CANCELLED
}

@MappedSuperclass
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var archived: Boolean = false

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

@Entity
@Table(name = "users")
class User : BaseEntity() {
    @Column(nullable = false, unique = true, length = 100)
    lateinit var username: String

    @Column(nullable = false, unique = true, length = 150)
    lateinit var email: String

    @Column(name = "full_name", nullable = false, length = 160)
    lateinit var fullName: String

    @Column(name = "password_hash", nullable = false, length = 255)
    lateinit var passwordHash: String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var role: Role = Role.TECHNICIAN

    @Column(nullable = false)
    var active: Boolean = true
}

@Entity
@Table(name = "refresh_tokens")
class RefreshToken : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    lateinit var user: User

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    lateinit var tokenHash: String

    @Column(name = "expires_at", nullable = false)
    lateinit var expiresAt: Instant

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
}

@Entity
@Table(name = "regions")
class Region : BaseEntity() {
    @Column(nullable = false, unique = true, length = 120)
    lateinit var name: String
}

@Entity
@Table(name = "client_companies")
class ClientCompany : BaseEntity() {
    @Column(nullable = false, unique = true, length = 160)
    lateinit var name: String

    @Column(name = "tax_id", nullable = false, unique = true, length = 30)
    lateinit var taxId: String

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    lateinit var region: Region
}

@Entity
@Table(name = "cedis")
class Cedis : BaseEntity() {
    @Column(nullable = false, unique = true, length = 160)
    lateinit var name: String

    @Column(nullable = false, unique = true, length = 150)
    lateinit var email: String

    @Column(nullable = false, length = 30)
    lateinit var phone: String

    @Column(name = "alternate_phone", nullable = false, length = 30)
    lateinit var alternatePhone: String
}

@Entity
@Table(name = "vehicle_units")
class VehicleUnit : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_company_id", nullable = false)
    lateinit var clientCompany: ClientCompany

    @Column(nullable = false, unique = true, length = 20)
    lateinit var plate: String

    @Column(nullable = false, unique = true, length = 30)
    lateinit var vin: String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var category: VehicleCategory = VehicleCategory.N2

    @Column(nullable = false, length = 80)
    lateinit var brand: String

    @Column(nullable = false, length = 80)
    lateinit var model: String
}

@Entity
@Table(name = "verification_orders")
class VerificationOrder : BaseEntity() {
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    lateinit var orderNumber: String

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_company_id", nullable = false)
    lateinit var clientCompany: ClientCompany

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    lateinit var region: Region

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_technician_id", nullable = false)
    lateinit var assignedTechnician: User

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: VerificationOrderStatus = VerificationOrderStatus.OPEN

    @Column(name = "scheduled_at", nullable = false)
    lateinit var scheduledAt: Instant

    @Column(columnDefinition = "TEXT")
    var notes: String? = null

    @OneToMany(mappedBy = "verificationOrder", cascade = [CascadeType.ALL], orphanRemoval = true)
    var units: MutableList<OrderUnit> = mutableListOf()
}

@Entity
@Table(name = "order_units")
class OrderUnit : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_order_id", nullable = false)
    lateinit var verificationOrder: VerificationOrder

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_unit_id", nullable = false)
    lateinit var vehicleUnit: VehicleUnit
}

@Entity
@Table(name = "checklist_templates")
class ChecklistTemplate : BaseEntity() {
    @Column(nullable = false, length = 160)
    lateinit var name: String

    @Column(nullable = false)
    var version: Int = 1

    @Column(nullable = false)
    var current: Boolean = true

    @OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    var sections: MutableList<ChecklistSection> = mutableListOf()
}

@Entity
@Table(name = "checklist_sections")
class ChecklistSection : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    lateinit var template: ChecklistTemplate

    @Column(nullable = false, length = 160)
    lateinit var title: String

    @Column(columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @OneToMany(mappedBy = "section", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    var questions: MutableList<ChecklistQuestion> = mutableListOf()
}

@Entity
@Table(name = "checklist_questions")
class ChecklistQuestion : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    lateinit var section: ChecklistSection

    @Column(nullable = false, unique = true, length = 50)
    lateinit var code: String

    @Column(nullable = false, columnDefinition = "TEXT")
    lateinit var prompt: String

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(nullable = false)
    var required: Boolean = true
}

@Entity
@Table(name = "inspections")
class Inspection : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_order_id", nullable = false)
    lateinit var verificationOrder: VerificationOrder

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_unit_id", nullable = false)
    lateinit var orderUnit: OrderUnit

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    lateinit var technician: User

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    lateinit var template: ChecklistTemplate

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: InspectionStatus = InspectionStatus.DRAFT

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_result", length = 20)
    var overallResult: InspectionResult? = null

    @Column(name = "overall_comment", columnDefinition = "TEXT")
    var overallComment: String? = null

    @Column(name = "last_section_index", nullable = false)
    var lastSectionIndex: Int = 0

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now()

    @Column(name = "submitted_at")
    var submittedAt: Instant? = null

    @OneToMany(mappedBy = "inspection", cascade = [CascadeType.ALL], orphanRemoval = true)
    var answers: MutableList<InspectionAnswer> = mutableListOf()

    @OneToMany(mappedBy = "inspection", cascade = [CascadeType.ALL], orphanRemoval = true)
    var sectionNotes: MutableList<InspectionSectionNote> = mutableListOf()

    @OneToMany(mappedBy = "inspection", cascade = [CascadeType.ALL], orphanRemoval = true)
    var evidences: MutableList<InspectionEvidence> = mutableListOf()
}

@Entity
@Table(name = "inspection_answers")
class InspectionAnswer : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    lateinit var inspection: Inspection

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    lateinit var question: ChecklistQuestion

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_value", nullable = false, length = 10)
    var answerValue: AnswerValue = AnswerValue.NA

    @Column(columnDefinition = "TEXT")
    var comment: String? = null
}

@Entity
@Table(name = "inspection_section_notes")
class InspectionSectionNote : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    lateinit var inspection: Inspection

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    lateinit var section: ChecklistSection

    @Column(columnDefinition = "TEXT")
    var comment: String? = null
}

@Entity
@Table(name = "inspection_evidences")
class InspectionEvidence : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    lateinit var inspection: Inspection

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    var section: ChecklistSection? = null

    @Column(nullable = false, length = 255)
    lateinit var filename: String

    @Column(name = "mime_type", nullable = false, length = 120)
    lateinit var mimeType: String

    @Column(nullable = false, length = 64)
    lateinit var checksum: String

    @Column(name = "captured_at", nullable = false)
    lateinit var capturedAt: Instant

    @Column(columnDefinition = "TEXT")
    var comment: String? = null

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    lateinit var content: ByteArray
}

@Entity
@Table(name = "payments")
class Payment : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verification_order_id", nullable = false)
    lateinit var verificationOrder: VerificationOrder

    @Column(nullable = false, precision = 12, scale = 2)
    lateinit var amount: BigDecimal

    @Column(nullable = false, length = 10)
    var currency: String = "MXN"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.PENDING

    @Column(length = 120)
    var reference: String? = null

    @Column(columnDefinition = "TEXT")
    var notes: String? = null

    @Column(name = "paid_at")
    var paidAt: Instant? = null
}

@Entity
@Table(name = "audit_log")
class AuditLog : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    var actorUser: User? = null

    @Column(nullable = false, length = 80)
    lateinit var action: String

    @Column(name = "entity_name", nullable = false, length = 80)
    lateinit var entityName: String

    @Column(name = "entity_id", nullable = false, length = 80)
    lateinit var entityId: String

    @Column(name = "details_json", columnDefinition = "TEXT")
    var detailsJson: String? = null
}
