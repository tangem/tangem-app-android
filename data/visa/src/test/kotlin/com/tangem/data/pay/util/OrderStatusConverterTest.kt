package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.spend.datasource.pay.models.response.OrderResponse.Result.Status
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderStatusConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN known status WHEN converted THEN mapped to the domain status`(model: ConvertModel) {
        // Act
        val actual = OrderStatusConverter.convert(model.status)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN unknown status WHEN converted THEN domain status is PROCESSING`() {
        // Act
        val actual = OrderStatusConverter.convert(Status.UNKNOWN)

        // Assert
        assertThat(actual).isEqualTo(OrderStatus.PROCESSING)
    }

    private fun provideTestModels() = listOf(
        ConvertModel(status = Status.NEW, expected = OrderStatus.NEW),
        ConvertModel(status = Status.PROCESSING, expected = OrderStatus.PROCESSING),
        ConvertModel(status = Status.COMPLETED, expected = OrderStatus.COMPLETED),
        ConvertModel(status = Status.CANCELED, expected = OrderStatus.CANCELED),
    )

    internal data class ConvertModel(val status: Status, val expected: OrderStatus)
}