package com.sivemor.platform.service

import com.sivemor.platform.common.BadRequestException
import com.sivemor.platform.domain.AnswerValue
import com.sivemor.platform.domain.Evaluacion
import com.sivemor.platform.domain.EvaluacionRepository
import com.sivemor.platform.domain.Inspection
import com.sivemor.platform.domain.InspectionSectionNote
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
    private val evaluacionRepository: EvaluacionRepository,
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

        val evaluacion = evaluacionRepository.findByVerificacionIdAndArchivedFalse(savedVerificacion.id ?: 0L)
            ?: Evaluacion().apply {
                this.verificacion = savedVerificacion
            }

        populateEvaluacion(inspection, evaluacion)
        evaluacionRepository.save(evaluacion)
        return savedVerificacion
    }

    private fun populateEvaluacion(inspection: Inspection, evaluacion: Evaluacion) {
        val answersByCode = inspection.answers.associateBy({ it.question.code.trim().lowercase() }, { it.answerValue })
        val notesBySection = inspection.sectionNotes.associateBy(
            { normalizeSectionName(it.section.title) },
            InspectionSectionNote::comment
        )

        evaluacion.lucesGalibo = mapAnswerEnum(answersByCode["luces_galibo"])
        evaluacion.lucesAltas = mapAnswerEnum(answersByCode["luces_altas"])
        evaluacion.lucesBajas = mapAnswerEnum(answersByCode["luces_bajas"])
        evaluacion.lucesDemarcadorasDelanteras = mapAnswerEnum(answersByCode["luces_demarcadoras_delanteras"])
        evaluacion.lucesDemarcadorasTraseras = mapAnswerEnum(answersByCode["luces_demarcadoras_traseras"])
        evaluacion.lucesIndicadoras = mapAnswerEnum(answersByCode["luces_indicadoras"])
        evaluacion.faroIzquierdo = mapAnswerEnum(answersByCode["faro_izquierdo"])
        evaluacion.faroDerecho = mapAnswerEnum(answersByCode["faro_derecho"])
        evaluacion.lucesDireccionalesDelanteras = mapAnswerEnum(answersByCode["luces_direccionales_delanteras"])
        evaluacion.lucesDireccionalesTraseras = mapAnswerEnum(answersByCode["luces_direccionales_traseras"])

        evaluacion.llantasRinesDelanteros = mapAnswerEnum(answersByCode["llantas_rines_delanteros"])
        evaluacion.llantasRinesTraseros = mapAnswerEnum(answersByCode["llantas_rines_traseros"])
        evaluacion.llantasMasasDelanteras = mapAnswerEnum(answersByCode["llantas_masas_delanteras"])
        evaluacion.llantasMasasTraseras = mapAnswerEnum(answersByCode["llantas_masas_traseras"])
        evaluacion.llantasPresionDelanteraIzquierda = parseDoubleAnswer(inspection, "llantas_presion_delantera_izquierda")
        evaluacion.llantasPresionDelanteraDerecha = parseDoubleAnswer(inspection, "llantas_presion_delantera_derecha")
        evaluacion.llantasPresionTraseraIzquierda1 = parseDoubleAnswer(inspection, "llantas_presion_trasera_izquierda_1")
        evaluacion.llantasPresionTraseraIzquierda2 = parseDoubleAnswer(inspection, "llantas_presion_trasera_izquierda_2")
        evaluacion.llantasPresionTraseraDerecha1 = parseDoubleAnswer(inspection, "llantas_presion_trasera_derecha_1")
        evaluacion.llantasPresionTraseraDerecha2 = parseDoubleAnswer(inspection, "llantas_presion_trasera_derecha_2")
        evaluacion.llantasProfundidadDelanteraIzquierda = parseDoubleAnswer(inspection, "llantas_profundidad_delantera_izquierda")
        evaluacion.llantasProfundidadDelanteraDerecha = parseDoubleAnswer(inspection, "llantas_profundidad_delantera_derecha")
        evaluacion.llantasProfundidadTraseraIzquierda1 = parseDoubleAnswer(inspection, "llantas_profundidad_trasera_izquierda_1")
        evaluacion.llantasProfundidadTraseraIzquierda2 = parseDoubleAnswer(inspection, "llantas_profundidad_trasera_izquierda_2")
        evaluacion.llantasProfundidadTraseraDerecha1 = parseDoubleAnswer(inspection, "llantas_profundidad_trasera_derecha_1")
        evaluacion.llantasProfundidadTraseraDerecha2 = parseDoubleAnswer(inspection, "llantas_profundidad_trasera_derecha_2")
        evaluacion.llantasTuercasDelanteraIzquierda = mapAnswerEnum(answersByCode["llantas_tuercas_delantera_izquierda"])
        evaluacion.llantasTuercasDelanteraIzquierdaFaltantes = parseIntAnswer(inspection, "llantas_tuercas_faltantes_delantera_izquierda")
        evaluacion.llantasTuercasDelanteraIzquierdaRotas = parseIntAnswer(inspection, "llantas_tuercas_rotas_delantera_izquierda")
        evaluacion.llantasTuercasDelanteraDerecha = mapAnswerEnum(answersByCode["llantas_tuercas_delantera_derecha"])
        evaluacion.llantasTuercasDelanteraDerechaFaltantes = parseIntAnswer(inspection, "llantas_tuercas_faltantes_delantera_derecha")
        evaluacion.llantasTuercasDelanteraDerechaRotas = parseIntAnswer(inspection, "llantas_tuercas_rotas_delantera_derecha")
        evaluacion.llantasTuercasTraseraIzquierda = mapAnswerEnum(answersByCode["llantas_tuercas_trasera_izquierda"])
        evaluacion.llantasTuercasTraseraIzquierdaFaltantes = parseIntAnswer(inspection, "llantas_tuercas_faltantes_trasera_izquierda")
        evaluacion.llantasTuercasTraseraIzquierdaRotas = parseIntAnswer(inspection, "llantas_tuercas_rotas_trasera_izquierda")
        evaluacion.llantasTuercasTraseraDerecha = mapAnswerEnum(answersByCode["llantas_tuercas_trasera_derecha"])
        evaluacion.llantasTuercasTraseraDerechaFaltantes = parseIntAnswer(inspection, "llantas_tuercas_faltantes_trasera_derecha")
        evaluacion.llantasTuercasTraseraDerechaRotas = parseIntAnswer(inspection, "llantas_tuercas_rotas_trasera_derecha")

        evaluacion.direccionBrazoPitman = mapAnswerEnum(answersByCode["direccion_brazo_pitman"])
        evaluacion.direccionManijasPuertas = mapAnswerEnum(answersByCode["direccion_manijas_puertas"])
        evaluacion.direccionChavetas = mapAnswerEnum(answersByCode["direccion_chavetas"])
        evaluacion.direccionChavetasFaltantes = parseIntAnswer(inspection, "direccion_chavetas_faltantes")

        evaluacion.aireFrenosCompresor = mapAnswerEnum(answersByCode["aire_frenos_compresor"])
        evaluacion.aireFrenosTanquesAire = mapAnswerEnum(answersByCode["aire_frenos_tanques_aire"])
        evaluacion.aireFrenosTiempoCargaPsi = parseDoubleAnswer(inspection, "aire_frenos_tiempo_carga_psi")
        evaluacion.aireFrenosTiempoCargaTiempo = parseDoubleAnswer(inspection, "aire_frenos_tiempo_carga_tiempo")

        evaluacion.motorEmisionesHumo = mapAnswerEnum(answersByCode["motor_emisiones_humo"])
        evaluacion.motorEmisionesGobernado = mapAnswerEnum(answersByCode["motor_emisiones_gobernado"])

        evaluacion.otrosCajaDireccion = mapAnswerEnum(answersByCode["otros_caja_direccion"])
        evaluacion.otrosDepositoAceite = mapAnswerEnum(answersByCode["otros_deposito_aceite"])
        evaluacion.otrosParabrisas = mapAnswerEnum(answersByCode["otros_parabrisas"])
        evaluacion.otrosLimpiaparabrisas = mapAnswerEnum(answersByCode["otros_limpiaparabrisas"])
        evaluacion.otrosJuego = mapAnswerEnum(answersByCode["otros_juego"])
        evaluacion.otrosEscape = mapAnswerEnum(answersByCode["otros_escape"])

        evaluacion.comentarioLuces = notesBySection["sistema_de_luces"] ?: notesBySection["luces"]
        evaluacion.comentarioLlantas = notesBySection["llantas"]
        evaluacion.comentarioDireccion = notesBySection["direccion_estructura_y_accesos"] ?: notesBySection["direccion"]
        evaluacion.comentarioAireFrenos = notesBySection["sistema_de_aire_frenos"] ?: notesBySection["aire_frenos"]
        evaluacion.comentarioMotorEmisiones = notesBySection["motor_y_emisiones"] ?: notesBySection["motor_emisiones"]
        evaluacion.comentarioOtros = notesBySection["otros"]
        evaluacion.comentariosGenerales = inspection.overallComment
        evaluacion.evidenceCount = inspection.evidences.size
    }

    private fun determineMateria(inspection: Inspection): VerificacionMateria = when {
        inspection.answers.any { it.question.code.trim().lowercase().startsWith("motor_emisiones_") } -> VerificacionMateria.HUMO
        else -> VerificacionMateria.GASOLINA
    }

    private fun mapAnswerEnum(answer: AnswerValue?): String? = when (answer) {
        AnswerValue.PASS -> "APROBADO"
        AnswerValue.FAIL -> "REPROBADO"
        AnswerValue.NA -> "NO_APLICA"
        null -> null
    }

    private fun parseDoubleAnswer(inspection: Inspection, code: String): Double? =
        inspection.answers
            .firstOrNull { it.question.code.trim().equals(code, ignoreCase = true) }
            ?.comment
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?: inspection.answers
                .firstOrNull { it.question.code.trim().equals(code, ignoreCase = true) }
                ?.let { answer ->
                    if (answer.answerValue == AnswerValue.NA) {
                        null
                    } else if (answer.comment.isNullOrBlank()) {
                        throw BadRequestException("Missing numeric value for $code")
                    } else {
                        throw BadRequestException("Invalid numeric value for $code")
                    }
                }

    private fun parseIntAnswer(inspection: Inspection, code: String): Int? =
        parseDoubleAnswer(inspection, code)?.toInt()

    private fun normalizeSectionName(value: String): String = value
        .trim()
        .lowercase()
        .replace("[^a-z0-9]+".toRegex(), "_")
        .trim('_')
}
