package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.spend.datasource.pay.addTangemPayEnumFallbackAdapters
import com.tangem.spend.datasource.pay.models.response.OrderResponse.Result.Status
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderResponseParsingTest {

    private val adapter = Moshi.Builder()
        .addTangemPayEnumFallbackAdapters()
        .add(RawJsonAdapter())
        .build()
        .adapter(OrderResponse.Result::class.java)

    @Test
    fun `GIVEN unknown status WHEN order parsed THEN status is UNKNOWN`() {
        // Arrange
        val json = orderJson(status = "\"AWAITING_SOMETHING_NEW\"")

        // Act
        val result = adapter.fromJson(json)!!

        // Assert
        assertThat(result.status).isEqualTo(Status.UNKNOWN)
        assertThat(result.id).isEqualTo(ORDER_ID)
    }

    @Test
    fun `GIVEN missing status WHEN order parsed THEN status is UNKNOWN`() {
        // Arrange
        val json = """{ "id": "$ORDER_ID", "data": {} }"""

        // Act
        val result = adapter.fromJson(json)!!

        // Assert
        assertThat(result.status).isEqualTo(Status.UNKNOWN)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN known statuses WHEN order parsed THEN mapped one to one`(model: StatusModel) {
        // Arrange
        val json = orderJson(status = "\"${model.wireValue}\"")

        // Act
        val result = adapter.fromJson(json)!!

        // Assert
        assertThat(result.status).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        StatusModel(wireValue = "NEW", expected = Status.NEW),
        StatusModel(wireValue = "PROCESSING", expected = Status.PROCESSING),
        StatusModel(wireValue = "COMPLETED", expected = Status.COMPLETED),
        StatusModel(wireValue = "CANCELED", expected = Status.CANCELED),
    )

    private fun orderJson(status: String) = """{ "id": "$ORDER_ID", "status": $status, "data": {} }"""

    internal data class StatusModel(val wireValue: String, val expected: Status)

    private companion object {
        const val ORDER_ID = "order_1"
    }
}