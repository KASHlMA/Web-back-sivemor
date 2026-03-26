package com.sivemor.platform.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.sivemor.platform.domain.AuditLog
import com.sivemor.platform.domain.AuditLogRepository
import com.sivemor.platform.domain.User
import org.springframework.stereotype.Service

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper
) {
    fun log(
        actor: User?,
        action: String,
        entityName: String,
        entityId: String,
        details: Any? = null
    ) {
        val auditLog = AuditLog().apply {
            actorUser = actor
            this.action = action
            this.entityName = entityName
            this.entityId = entityId
            this.detailsJson = details?.let { objectMapper.writeValueAsString(it) }
        }
        auditLogRepository.save(auditLog)
    }
}
