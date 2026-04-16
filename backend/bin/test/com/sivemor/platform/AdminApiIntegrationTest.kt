package com.sivemor.platform

import com.sivemor.platform.support.createClient
import com.sivemor.platform.support.createVehicle
import com.sivemor.platform.support.createRegion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class AdminApiIntegrationTest : IntegrationTestSupport() {
    @Test
    fun `admin can create update archive and stop listing a region`() {
        val adminToken = bearerTokenForUser("admin")
        val suffix = System.currentTimeMillis()

        val regionId = createRegion("Region $suffix", adminToken)

        mockMvc.put("/api/v1/admin/regions/$regionId") {
            header("Authorization", adminToken)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Region $suffix Updated"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Region $suffix Updated") }
        }

        val beforeDelete = mockMvc.get("/api/v1/admin/regions") {
            header("Authorization", adminToken)
        }.andExpect {
            status { isOk() }
        }.andReturn()
        assertThat(beforeDelete.response.contentAsString).contains("Region $suffix Updated")

        mockMvc.delete("/api/v1/admin/regions/$regionId") {
            header("Authorization", adminToken)
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.get("/api/v1/admin/regions") {
            header("Authorization", adminToken)
        }.andExpect {
            status { isOk() }
            content { string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Region $suffix Updated"))) }
        }
    }

    @Test
    fun `admin endpoints enforce validation and role restrictions`() {
        val adminToken = bearerTokenForUser("admin")
        val technicianToken = bearerTokenForUser("tecnico1")

        mockMvc.post("/api/v1/admin/users") {
            header("Authorization", adminToken)
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"","email":"invalid","fullName":"","role":"ADMIN","active":true}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Validation failed") }
            jsonPath("$.details.username") { exists() }
            jsonPath("$.details.email") { exists() }
        }

        mockMvc.get("/api/v1/admin/users") {
            header("Authorization", technicianToken)
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `updating a vehicle with duplicate plate returns bad request instead of server error`() {
        val adminToken = bearerTokenForUser("admin")
        val suffix = System.currentTimeMillis()
        val regionId = createRegion("Region Vehicle $suffix", adminToken)
        val clientId = createClient("Cliente Vehicle $suffix", "RFC$suffix", regionId, adminToken)
        val vehicleOneId = createVehicle(clientId, "VEH-$suffix-A", "VIN-$suffix-A", adminToken)
        createVehicle(clientId, "VEH-$suffix-B", "VIN-$suffix-B", adminToken)

        mockMvc.put("/api/v1/admin/vehicles/$vehicleOneId") {
            header("Authorization", adminToken)
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "clientCompanyId": $clientId,
                  "plate": "VEH-$suffix-B",
                  "vin": "VIN-$suffix-A",
                  "category": "N2",
                  "brand": "Freightliner",
                  "model": "M2 106"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Ya existe un vehiculo con esa placa") }
        }
    }
}
