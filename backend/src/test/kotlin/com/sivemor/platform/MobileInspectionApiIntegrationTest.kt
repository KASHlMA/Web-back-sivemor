package com.sivemor.platform

import com.sivemor.platform.support.answerAllQuestions
import com.sivemor.platform.support.createClient
import com.sivemor.platform.support.createDraftInspection
import com.sivemor.platform.support.createOrder
import com.sivemor.platform.support.createRegion
import com.sivemor.platform.support.createVehicle
import com.sivemor.platform.support.fetchTechnicianOrderUnits
import com.sivemor.platform.support.findUserId
import com.sivemor.platform.support.submitInspection
import com.sivemor.platform.support.updateDraftInspection
import com.sivemor.platform.support.uploadEvidence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get

class MobileInspectionApiIntegrationTest : IntegrationTestSupport() {
    @Autowired
    lateinit var verificacionRepository: com.sivemor.platform.domain.VerificacionRepository

    @Autowired
    lateinit var evaluacionRepository: com.sivemor.platform.domain.EvaluacionRepository

    @Test
    fun `technician can move from draft to submission and complete the order after both units are submitted`() {
        val adminToken = bearerTokenForUser("admin")
        val technicianToken = bearerTokenForUser("tecnico1")
        val suffix = System.currentTimeMillis()

        val technicianId = findUserId("tecnico1", adminToken)
        val regionId = createRegion("Mobile Region $suffix", adminToken)
        val clientId = createClient("Mobile Client $suffix", "MCL$suffix", regionId, adminToken)
        val vehicleOneId = createVehicle(clientId, "MOB-$suffix-1", "VIN-$suffix-1", adminToken)
        val vehicleTwoId = createVehicle(clientId, "MOB-$suffix-2", "VIN-$suffix-2", adminToken)
        val orderNumber = "MOBILE-$suffix"

        createOrder(orderNumber, clientId, regionId, technicianId, listOf(vehicleOneId, vehicleTwoId), adminToken)

        val orderUnitIds = fetchTechnicianOrderUnits(orderNumber, technicianToken)
        assertThat(orderUnitIds).hasSize(2)

        orderUnitIds.forEachIndexed { index, orderUnitId ->
            val draft = createDraftInspection(orderUnitId, technicianToken)
            val inspectionId = draft["id"].asLong()

            if (index == 0) {
                val failedSubmit = submitInspection(inspectionId, technicianToken)
                assertThat(failedSubmit["message"].asText()).isEqualTo("Required questions are still unanswered")
            }

            updateDraftInspection(inspectionId, answerAllQuestions(draft), technicianToken)
            val sectionId = draft["sections"][0]["sectionId"].asLong()
            uploadEvidence(inspectionId, sectionId, technicianToken, "evidence-$suffix-${index}-1.jpg")
            uploadEvidence(inspectionId, sectionId, technicianToken, "evidence-$suffix-${index}-2.jpg")
            uploadEvidence(inspectionId, sectionId, technicianToken, "evidence-$suffix-${index}-3.jpg")

            val submitted = submitInspection(inspectionId, technicianToken)
            assertThat(submitted["status"].asText()).isEqualTo("SUBMITTED")
        }

        mockMvc.get("/api/v1/admin/orders") {
            header("Authorization", adminToken)
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.orderNumber=='$orderNumber')].status") { value(org.hamcrest.Matchers.hasItem("COMPLETED")) }
        }
    }

    @Test
    fun `submitted inspections expose MER detail and reports fallback remains compatible`() {
        val adminToken = bearerTokenForUser("admin")
        val technicianToken = bearerTokenForUser("tecnico1")
        val suffix = System.currentTimeMillis()

        val technicianId = findUserId("tecnico1", adminToken)
        val regionId = createRegion("MER Region $suffix", adminToken)
        val clientId = createClient("MER Client $suffix", "MER$suffix", regionId, adminToken)
        val vehicleId = createVehicle(clientId, "MER-$suffix", "MER-VIN-$suffix", adminToken)
        val orderNumber = "MER-ORDER-$suffix"

        createOrder(orderNumber, clientId, regionId, technicianId, listOf(vehicleId), adminToken)
        val orderUnitId = fetchTechnicianOrderUnits(orderNumber, technicianToken).single()
        val draft = createDraftInspection(orderUnitId, technicianToken)
        val inspectionId = draft["id"].asLong()

        updateDraftInspection(inspectionId, answerAllQuestions(draft), technicianToken)
        val sectionId = draft["sections"][0]["sectionId"].asLong()
        uploadEvidence(inspectionId, sectionId, technicianToken, "mer-$suffix-1.jpg")
        uploadEvidence(inspectionId, sectionId, technicianToken, "mer-$suffix-2.jpg")
        uploadEvidence(inspectionId, sectionId, technicianToken, "mer-$suffix-3.jpg")
        submitInspection(inspectionId, technicianToken)

        mockMvc.get("/api/v1/admin/reports/$inspectionId") {
            header("Authorization", adminToken)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.verificacionId") { exists() }
            jsonPath("$.inspectionId") { value(inspectionId.toInt()) }
            jsonPath("$.source") { value("MER") }
        }

        mockMvc.get("/api/v1/admin/web-verifications") {
            header("Authorization", adminToken)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].verificacionId") { exists() }
            jsonPath("$[0].noteNumber") { value(orderNumber) }
        }

        mockMvc.get("/api/v1/admin/reports?orderId=&onlyFailures=false") {
            header("Authorization", adminToken)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.inspectionId==${inspectionId.toInt()})].orderNumber") {
                value(org.hamcrest.Matchers.hasItem(orderNumber))
            }
        }
    }

    @Test
    fun `web verifications recreate MER rows for submitted inspections already stored in base`() {
        val adminToken = bearerTokenForUser("admin")
        val technicianToken = bearerTokenForUser("tecnico1")
        val suffix = System.currentTimeMillis()

        val technicianId = findUserId("tecnico1", adminToken)
        val regionId = createRegion("MER Sync Region $suffix", adminToken)
        val clientId = createClient("MER Sync Client $suffix", "MSR$suffix", regionId, adminToken)
        val vehicleId = createVehicle(clientId, "SYNC-$suffix", "SYNC-VIN-$suffix", adminToken)
        val orderNumber = "SYNC-MER-$suffix"

        createOrder(orderNumber, clientId, regionId, technicianId, listOf(vehicleId), adminToken)
        val orderUnitId = fetchTechnicianOrderUnits(orderNumber, technicianToken).single()
        val draft = createDraftInspection(orderUnitId, technicianToken)
        val inspectionId = draft["id"].asLong()

        updateDraftInspection(inspectionId, answerAllQuestions(draft), technicianToken)
        val sectionId = draft["sections"][0]["sectionId"].asLong()
        uploadEvidence(inspectionId, sectionId, technicianToken, "sync-mer-$suffix-1.jpg")
        uploadEvidence(inspectionId, sectionId, technicianToken, "sync-mer-$suffix-2.jpg")
        uploadEvidence(inspectionId, sectionId, technicianToken, "sync-mer-$suffix-3.jpg")
        submitInspection(inspectionId, technicianToken)

        val existingVerification = requireNotNull(verificacionRepository.findByInspectionIdAndArchivedFalse(inspectionId))
        evaluacionRepository.findByVerificacionIdAndArchivedFalse(existingVerification.id ?: 0L)?.let { evaluacionRepository.delete(it) }
        verificacionRepository.delete(existingVerification)

        mockMvc.get("/api/v1/admin/web-verifications") {
            header("Authorization", adminToken)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.inspectionId==${inspectionId.toInt()})].noteNumber") {
                value(org.hamcrest.Matchers.hasItem(orderNumber))
            }
        }

        assertThat(verificacionRepository.findByInspectionIdAndArchivedFalse(inspectionId)).isNotNull()
    }

    @Test
    fun `admin can archive a web verification and it disappears from admin listings`() {
        val adminToken = bearerTokenForUser("admin")
        val technicianToken = bearerTokenForUser("tecnico1")
        val suffix = System.currentTimeMillis()

        val technicianId = findUserId("tecnico1", adminToken)
        val regionId = createRegion("Delete MER Region $suffix", adminToken)
        val clientId = createClient("Delete MER Client $suffix", "DMER$suffix", regionId, adminToken)
        val vehicleId = createVehicle(clientId, "DEL-$suffix", "DEL-VIN-$suffix", adminToken)
        val orderNumber = "DELETE-MER-$suffix"

        createOrder(orderNumber, clientId, regionId, technicianId, listOf(vehicleId), adminToken)
        val orderUnitId = fetchTechnicianOrderUnits(orderNumber, technicianToken).single()
        val draft = createDraftInspection(orderUnitId, technicianToken)
        val inspectionId = draft["id"].asLong()

        updateDraftInspection(inspectionId, answerAllQuestions(draft), technicianToken)
        val sectionId = draft["sections"][0]["sectionId"].asLong()
        uploadEvidence(inspectionId, sectionId, technicianToken, "delete-mer-$suffix-1.jpg")
        uploadEvidence(inspectionId, sectionId, technicianToken, "delete-mer-$suffix-2.jpg")
        uploadEvidence(inspectionId, sectionId, technicianToken, "delete-mer-$suffix-3.jpg")
        submitInspection(inspectionId, technicianToken)

        val webVerificationId = mockMvc.get("/api/v1/admin/web-verifications") {
            header("Authorization", adminToken)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.inspectionId==${inspectionId.toInt()})].noteNumber") {
                value(org.hamcrest.Matchers.hasItem(orderNumber))
            }
        }.andReturn().response.contentAsString.let { body ->
            objectMapper.readTree(body)
                .first { it["inspectionId"].asLong() == inspectionId }["verificacionId"].asLong()
        }

        mockMvc.delete("/api/v1/admin/web-verifications/$webVerificationId") {
            header("Authorization", adminToken)
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/v1/admin/web-verifications") {
            header("Authorization", adminToken)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.verificacionId==${webVerificationId.toInt()})]") { isEmpty() }
        }

        mockMvc.get("/api/v1/admin/reports?orderId=&onlyFailures=false") {
            header("Authorization", adminToken)
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.inspectionId==${inspectionId.toInt()})]") { isEmpty() }
        }
    }
}
