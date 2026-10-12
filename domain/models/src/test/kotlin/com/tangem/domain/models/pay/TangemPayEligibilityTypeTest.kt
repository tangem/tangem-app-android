package com.tangem.domain.models.pay

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayEligibilityTypeTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN channel value WHEN fromString THEN mapped type returned`(model: FromStringModel) {
        // Act
        val actual = TangemPayEligibilityType.fromString(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        FromStringModel(value = "BANK_OFFRAMP", expected = TangemPayEligibilityType.BANK_OFFRAMP),
        FromStringModel(value = "bank_offramp", expected = TangemPayEligibilityType.BANK_OFFRAMP),
        FromStringModel(value = "VISA_VIRTUAL_ACCOUNT", expected = TangemPayEligibilityType.VISA_VIRTUAL_ACCOUNT),
        FromStringModel(value = "SOMETHING_NEW", expected = TangemPayEligibilityType.UNKNOWN),
    )

    internal data class FromStringModel(val value: String, val expected: TangemPayEligibilityType)
}