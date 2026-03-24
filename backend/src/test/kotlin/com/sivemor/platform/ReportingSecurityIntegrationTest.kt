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
import org.springframework.test.web.servlet.get

class ReportingSecurityIntegrationTest : IntegrationTestSupport() {
    @Test
    fun `security rules and reporting endpoints reflect submitted pass and fail inspections`() {
        mockMvc.get("/api/v1/admin/users").andExpect {
            status { isForbidden() }
        }

        val adminToken = bearerTokenForUser("admin")
        val technicianToken = bearerTokenForUser("tecnico1")
        val suffix = System.currentTimeMillis()

        mockMvc.get("/api/v1/admin/users") {
            header("Authorization", technicianToken)
        }.andExpect {
            status { isForbidden() }
        }

        val technicianId = findUserId("tecnico1", adminToken)
        val regionId = createRegion("Reports Region $suffix", adminToken)
        val clientId = createClient("Reports Client $suffix", "RCL$suffix", regionId, adminToken)
        val vehicleOneId = createVehicle(clientId, "REP-$suffix-1", "R-VIN-$suffix-1", adminToken)
        val vehicleTwoId = createVehicle(clientId, "REP-$suffix-2", "R-VIN-$suffix-2", adminToken)
        val orderNumber = "REPORT-$suffix"

        createOrder(orderNumber, clientId, regionId, technicianId, listOf(vehicleOneId, vehicleTwoId), adminToken)

        fetchTechnicianOrderUnits(orderNumber, technicianToken).forEachIndexed { index, orderUnitId ->
            val draft = createDraftInspection(orderUnitId, technicianToken)
            val inspectionId = draft["id"].asLong()
            updateDraftInspection(inspectionId, answerAllQuestions(draft, failLastQuestion = index == 0), technicianToken)
            val sectionId = draft["sections"][0]["sectionId"].asLong()
            uploadEvidence(inspectionId, sectionId, technicianToken, "report-$suffix-${index}-1.jpg")
            uploadEvidence(inspectionId, sectionId, technicianToken, "report-$suffix-${index}-2.jpg")
            uploadEvidence(inspectionId, sectionId, technicianToken, "report-$suffix-${index}-3.jpg")

            val submitted = submitInspection(inspectionId, technicianToken)
            assertThat(submitted["status"].asText()).isEqualTo("SUBMITTED")
        }

        mockMvc.get("/api/v1/admin/reports") {
            header("Authorization", adminToken)
            param("onlyFailures", "true")
        }.andExpect {
            status { isOk() }
            jsonPath("$[?(@.orderNumber=='$orderNumber')]") { isArray() }
            jsonPath("$[?(@.orderNumber=='$orderNumber')].overallResult") { value(org.hamcrest.Matchers.hasItem("FAIL")) }
        }

        mockMvc.get("/api/v1/admin/dashboard/failures") {
            header("Authorization", adminToken)
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalSubmitted") { value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)) }
            jsonPath("$.totalFailed") { value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)) }
            jsonPath("$.recentFailures[?(@.orderNumber=='$orderNumber')]") { isArray() }
        }
    }
}
