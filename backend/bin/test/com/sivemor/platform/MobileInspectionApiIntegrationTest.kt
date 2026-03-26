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
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get

class MobileInspectionApiIntegrationTest : IntegrationTestSupport() {
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
}
