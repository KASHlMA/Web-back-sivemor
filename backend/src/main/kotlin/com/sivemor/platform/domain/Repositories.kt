package com.sivemor.platform.domain

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsernameIgnoreCaseAndArchivedFalse(username: String): User?
    fun findAllByArchivedFalseOrderByFullNameAsc(): List<User>
}

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHashAndRevokedAtIsNullAndArchivedFalse(tokenHash: String): RefreshToken?

    @Query(
        """
        select token from RefreshToken token
        where token.user.id = :userId and token.revokedAt is null and token.archived = false
        """
    )
    fun findAllActiveByUserId(userId: Long): List<RefreshToken>
}

interface RegionRepository : JpaRepository<Region, Long> {
    fun findAllByArchivedFalseOrderByNameAsc(): List<Region>
}

interface ClientCompanyRepository : JpaRepository<ClientCompany, Long> {
    fun findAllByArchivedFalseOrderByNameAsc(): List<ClientCompany>
}

interface VehicleUnitRepository : JpaRepository<VehicleUnit, Long> {
    fun findAllByArchivedFalseOrderByPlateAsc(): List<VehicleUnit>
}

interface VerificationOrderRepository : JpaRepository<VerificationOrder, Long> {
    fun findAllByArchivedFalseOrderByScheduledAtDesc(): List<VerificationOrder>
}

interface OrderUnitRepository : JpaRepository<OrderUnit, Long> {
    fun findByIdAndArchivedFalse(id: Long): OrderUnit?
    fun countByVerificationOrderIdAndArchivedFalse(verificationOrderId: Long): Long

    @EntityGraph(attributePaths = ["verificationOrder", "verificationOrder.clientCompany", "verificationOrder.region", "vehicleUnit"])
    @Query(
        """
        select orderUnit from OrderUnit orderUnit
        join orderUnit.verificationOrder ord
        where ord.assignedTechnician.id = :technicianId
          and orderUnit.archived = false
          and ord.archived = false
        order by ord.scheduledAt desc
        """
    )
    fun findAllAssignedToTechnician(technicianId: Long): List<OrderUnit>
}

interface ChecklistTemplateRepository : JpaRepository<ChecklistTemplate, Long> {
    @EntityGraph(attributePaths = ["sections", "sections.questions"])
    fun findByCurrentTrueAndArchivedFalse(): ChecklistTemplate?
}

interface ChecklistSectionRepository : JpaRepository<ChecklistSection, Long>

interface ChecklistQuestionRepository : JpaRepository<ChecklistQuestion, Long> {
    fun findAllBySectionTemplateIdAndArchivedFalse(templateId: Long): List<ChecklistQuestion>
}

interface InspectionRepository : JpaRepository<Inspection, Long> {
    @EntityGraph(
        attributePaths = [
            "verificationOrder",
            "verificationOrder.clientCompany",
            "verificationOrder.region",
            "orderUnit",
            "orderUnit.vehicleUnit",
            "technician",
            "template",
            "template.sections",
            "template.sections.questions",
            "answers",
            "answers.question",
            "sectionNotes",
            "sectionNotes.section",
            "evidences",
            "evidences.section"
        ]
    )
    @Query("select inspection from Inspection inspection where inspection.id = :id and inspection.archived = false")
    fun findDetailedById(id: Long): Inspection?

    fun findByOrderUnitIdAndArchivedFalse(orderUnitId: Long): Inspection?

    @EntityGraph(
        attributePaths = [
            "verificationOrder",
            "verificationOrder.clientCompany",
            "verificationOrder.region",
            "orderUnit",
            "orderUnit.vehicleUnit",
            "technician",
            "answers",
            "evidences"
        ]
    )
    fun findAllByStatusAndArchivedFalseOrderBySubmittedAtDesc(status: InspectionStatus): List<Inspection>

    fun countByVerificationOrderIdAndStatusAndArchivedFalse(
        verificationOrderId: Long,
        status: InspectionStatus
    ): Long
}

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findAllByArchivedFalseOrderByCreatedAtDesc(): List<Payment>
}

interface AuditLogRepository : JpaRepository<AuditLog, Long>
