package com.sivemor.platform.service

import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.Inspection
import com.sivemor.platform.domain.Verificacion
import com.sivemor.platform.domain.VerificacionMateria
import com.sivemor.platform.domain.VerificacionRepository
import com.sivemor.platform.domain.VerificacionVeredicto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class MerCompatibilityService(
    private val verificacionRepository: VerificacionRepository,
    private val clock: Clock
) {
    @Transactional
    fun syncSubmittedInspection(inspection: Inspection): Verificacion {
        val existing = verificacionRepository.findByInspectionIdAndArchivedFalse(inspection.id ?: 0L)
        val verificacion = existing ?: Verificacion().apply {
            this.inspection = inspection
            vehicleUnit = inspection.orderUnit.vehicleUnit
            verificationOrder = inspection.verificationOrder
            verificationCenter = null
            folioVerificacion = "VER-${inspection.id}"
        }

        verificacion.vehicleUnit = inspection.orderUnit.vehicleUnit
        verificacion.verificationOrder = inspection.verificationOrder
        verificacion.fechaVerificacion = inspection.submittedAt ?: Instant.now(clock)
        verificacion.veredicto = if (inspection.answers.any { it.answerValue == AnswerValue.FAIL }) {
            VerificacionVeredicto.REPROBADO
        } else {
            VerificacionVeredicto.APROBADO
        }
        verificacion.overallComment = inspection.overallComment
        verificacion.materia = determineMateria(inspection)

        val savedVerificacion = verificacionRepository.save(verificacion)
        return savedVerificacion
    }

    private fun determineMateria(inspection: Inspection): VerificacionMateria = when {
        inspection.answers.any { it.question.code.trim().lowercase().startsWith("motor_emisiones_") } -> VerificacionMateria.HUMO
        else -> VerificacionMateria.GASOLINA
    }
}
