package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.spend.datasource.pay.models.response.CustomerMeResponse.ProductInstance
import org.junit.jupiter.api.Test

internal class CustomerMeProductInstanceParsingTest {

    private val json = NetworkJsonFactory.create()

    @Test
    fun `GIVEN known status and specification type WHEN parsed THEN mapped one to one`() {
        // Arrange
        val raw = productInstanceJson(status = "\"ACTIVE\"", specificationType = "\"CARD\"")

        // Act
        val actual = json.decodeFromString(ProductInstance.serializer(), raw)

        // Assert
        assertThat(actual.status).isEqualTo(ProductInstance.Status.ACTIVE)
        assertThat(actual.specificationDataType).isEqualTo(ProductInstance.SpecificationDataType.CARD)
    }

    @Test
    fun `GIVEN unknown status and specification type WHEN parsed THEN both fall back to UNKNOWN`() {
        // Arrange
        val raw = productInstanceJson(status = "\"SUSPENDED\"", specificationType = "\"WEARABLE\"")

        // Act
        val actual = json.decodeFromString(ProductInstance.serializer(), raw)

        // Assert
        assertThat(actual.id).isEqualTo(INSTANCE_ID)
        assertThat(actual.status).isEqualTo(ProductInstance.Status.UNKNOWN)
        assertThat(actual.specificationDataType).isEqualTo(ProductInstance.SpecificationDataType.UNKNOWN)
    }

    @Test
    fun `GIVEN missing status and specification type WHEN parsed THEN both fall back to UNKNOWN`() {
        // Arrange
        val raw = """{"id":"$INSTANCE_ID","updated_at":"2026-01-02T03:04:05Z","payment_account_id":"pa-1"}"""

        // Act
        val actual = json.decodeFromString(ProductInstance.serializer(), raw)

        // Assert
        assertThat(actual.status).isEqualTo(ProductInstance.Status.UNKNOWN)
        assertThat(actual.specificationDataType).isEqualTo(ProductInstance.SpecificationDataType.UNKNOWN)
    }

    private fun productInstanceJson(status: String, specificationType: String): String =
        """{"id":"$INSTANCE_ID","status":$status,"updated_at":"2026-01-02T03:04:05Z",""" +
            """"payment_account_id":"pa-1","product_specification_data_type":$specificationType}"""

    private companion object {
        const val INSTANCE_ID = "pi-1"
    }
}