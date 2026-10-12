package com.tangem.domain.pay.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderStepTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN wire value WHEN fromString THEN mapped step returned`(model: FromStringModel) {
        // Act
        val actual = OrderStep.fromString(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FromStringModel(value = "AWAITING_DEPOSIT", expected = OrderStep.AWAITING_DEPOSIT),
        FromStringModel(value = "AWAITING_SIGNATURE", expected = OrderStep.AWAITING_SIGNATURE),
        FromStringModel(value = "awaiting_signature", expected = OrderStep.AWAITING_SIGNATURE),
        FromStringModel(value = "SOMETHING_NEW", expected = OrderStep.UNKNOWN),
        FromStringModel(value = null, expected = OrderStep.UNKNOWN),
    )

    internal data class FromStringModel(val value: String?, val expected: OrderStep)
}