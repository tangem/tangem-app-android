package com.tangem.domain.pay.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderTypeTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN wire value WHEN fromString THEN mapped type returned`(model: FromStringModel) {
        // Act
        val actual = OrderType.fromString(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FromStringModel(value = "BANK_OFFRAMP_RAIN", expected = OrderType.BANK_OFFRAMP_RAIN),
        FromStringModel(value = "WITHDRAW", expected = OrderType.WITHDRAW),
        FromStringModel(value = "SOMETHING_NEW", expected = OrderType.UNKNOWN),
        FromStringModel(value = "", expected = OrderType.UNKNOWN),
        FromStringModel(value = null, expected = OrderType.UNKNOWN),
    )

    internal data class FromStringModel(val value: String?, val expected: OrderType)
}