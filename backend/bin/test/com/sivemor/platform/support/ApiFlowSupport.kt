package com.sivemor.platform.support

import com.fasterxml.jackson.databind.JsonNode
import com.sivemor.platform.IntegrationTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant

fun IntegrationTestSupport.findUserId(username: String, bearerToken: String): Long {
    val result = mockMvc.get("/api/v1/admin/users") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
    }.andExpect {
        status { isOk() }
    }.andReturn()

    val users = objectMapper.readTree(result.response.contentAsString)
    return users.first { it["username"].asText() == username }["id"].asLong()
}

fun IntegrationTestSupport.createRegion(name: String, bearerToken: String): Long {
    val result = mockMvc.post("/api/v1/admin/regions") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(mapOf("name" to name))
    }.andExpect {
        status { isCreated() }
    }.andReturn()

    return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
}

fun IntegrationTestSupport.createClient(name: String, taxId: String, regionId: Long, bearerToken: String): Long {
    val result = mockMvc.post("/api/v1/admin/clients") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(
            mapOf(
                "name" to name,
                "businessName" to "$name SA de CV",
                "email" to "${taxId.lowercase()}@example.com",
                "phone" to "7774501100",
                "alternatePhone" to "7774502200",
                "manager" to "Gestor de prueba"
            )
        )
    }.andExpect {
        status { isCreated() }
    }.andReturn()

    return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
}

fun IntegrationTestSupport.createVehicle(
    clientCompanyId: Long,
    plate: String,
    vin: String,
    bearerToken: String
): Long {
    val result = mockMvc.post("/api/v1/admin/vehicles") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(
            mapOf(
                "clientCompanyId" to clientCompanyId,
                "plate" to plate,
                "vin" to vin,
                "category" to "N2",
                "brand" to "Freightliner",
                "model" to "M2 106"
            )
        )
    }.andExpect {
        status { isCreated() }
    }.andReturn()

    return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
}

fun IntegrationTestSupport.createOrder(
    orderNumber: String,
    clientCompanyId: Long,
    regionId: Long,
    assignedTechnicianId: Long,
    unitIds: List<Long>,
    bearerToken: String
): Long {
    val result = mockMvc.post("/api/v1/admin/orders") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(
            mapOf(
                "orderNumber" to orderNumber,
                "clientCompanyId" to clientCompanyId,
                "regionId" to regionId,
                "assignedTechnicianId" to assignedTechnicianId,
                "unitIds" to unitIds,
                "scheduledAt" to Instant.parse("2030-01-02T12:00:00Z").toString()
            )
        )
    }.andExpect {
        status { isCreated() }
    }.andReturn()

    return objectMapper.readTree(result.response.contentAsString)["id"].asLong()
}

fun IntegrationTestSupport.fetchTechnicianOrderUnits(orderNumber: String, bearerToken: String): List<Long> {
    val result = mockMvc.get("/api/v1/mobile/orders") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
    }.andExpect {
        status { isOk() }
    }.andReturn()

    val orders = objectMapper.readTree(result.response.contentAsString)
    return orders.filter { it["orderNumber"].asText() == orderNumber }
        .map { it["orderUnitId"].asLong() }
}

fun IntegrationTestSupport.createDraftInspection(orderUnitId: Long, bearerToken: String): JsonNode {
    val result = mockMvc.post("/api/v1/mobile/inspections") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(mapOf("orderUnitId" to orderUnitId))
    }.andExpect {
        status { isCreated() }
    }.andReturn()

    return objectMapper.readTree(result.response.contentAsString)
}

fun IntegrationTestSupport.answerAllQuestions(inspection: JsonNode, failLastQuestion: Boolean = false): String {
    val sections = inspection["sections"].map { section ->
        val questions = section["questions"].mapIndexed { index, question ->
            mapOf(
                "questionId" to question["id"].asLong(),
                "answer" to if (failLastQuestion && index == section["questions"].size() - 1) "FAIL" else "PASS",
                "comment" to "Checked ${question["code"].asText()}"
            )
        }

        mapOf(
            "sectionId" to section["sectionId"].asLong(),
            "note" to "Completed ${section["title"].asText()}",
            "questions" to questions
        )
    }

    return objectMapper.writeValueAsString(
        mapOf(
            "lastSectionIndex" to (inspection["sections"].size() - 1),
            "overallComment" to "Integration validation",
            "sections" to sections
        )
    )
}

fun IntegrationTestSupport.updateDraftInspection(inspectionId: Long, payload: String, bearerToken: String) {
    mockMvc.put("/api/v1/mobile/inspections/$inspectionId") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
        contentType = MediaType.APPLICATION_JSON
        content = payload
    }.andExpect {
        status { isOk() }
    }
}

fun IntegrationTestSupport.uploadEvidence(inspectionId: Long, sectionId: Long, bearerToken: String, fileName: String) {
    val file = MockMultipartFile("file", fileName, "image/jpeg", fileName.encodeToByteArray())
    mockMvc.perform(
        multipart("/api/v1/mobile/inspections/$inspectionId/evidences")
            .file(file)
            .param("sectionId", sectionId.toString())
            .param("comment", "Uploaded $fileName")
            .header(HttpHeaders.AUTHORIZATION, bearerToken)
    ).andExpect(status().isOk)
}

fun IntegrationTestSupport.submitInspection(inspectionId: Long, bearerToken: String): JsonNode {
    val result = mockMvc.post("/api/v1/mobile/inspections/$inspectionId/submit") {
        header(HttpHeaders.AUTHORIZATION, bearerToken)
    }.andReturn()

    assertThat(result.response.status).isIn(200, 400)
    return objectMapper.readTree(result.response.contentAsString)
}
