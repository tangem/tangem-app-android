package com.tangem.data.staking.converters.error

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.stakekit.StakingError.StakeKitApiError.ErrorDetails
import com.tangem.grow.datasource.stakekit.models.response.model.error.StakeKitErrorDetailsDTO
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakeKitErrorDetailsConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN details amount WHEN convert THEN amount parsed or null`(model: DetailsModel) {
        // Arrange
        val dto = StakeKitErrorDetailsDTO(arguments = "a", amount = model.amount, yieldId = "y")

        // Act
        val actual = StakeKitErrorDetailsConverter.convert(dto)

        // Assert
        assertThat(actual).isEqualTo(ErrorDetails(model.expected))
    }

    private fun provideTestModels() = listOf(
        DetailsModel("0.5", BigDecimal("0.5")),
        DetailsModel("abc", null),
        DetailsModel(null, null),
    )

    internal data class DetailsModel(val amount: String?, val expected: BigDecimal?)
}