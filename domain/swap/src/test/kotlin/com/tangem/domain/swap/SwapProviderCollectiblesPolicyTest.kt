package com.tangem.domain.swap

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SwapProviderCollectiblesPolicyTest {

    @ParameterizedTest
    @MethodSource("provideTopUpModels")
    fun `GIVEN provider id WHEN isTopUpCexAllowed THEN returns expected`(model: IsCexAllowedModel) {
        // Act
        val actual = SwapProviderCollectiblesPolicy.isTopUpCexAllowed(providerId = model.providerId)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideWithdrawalModels")
    fun `GIVEN provider id WHEN isWithdrawalCexAllowed THEN returns expected`(model: IsCexAllowedModel) {
        // Act
        val actual = SwapProviderCollectiblesPolicy.isWithdrawalCexAllowed(providerId = model.providerId)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class IsCexAllowedModel(val providerId: String, val expected: Boolean)

    private fun provideTopUpModels() = listOf(
        IsCexAllowedModel(providerId = "changehero", expected = true),
        IsCexAllowedModel(providerId = "changelly", expected = false),
        IsCexAllowedModel(providerId = "simpleswap", expected = false),
    )

    private fun provideWithdrawalModels() = listOf(
        IsCexAllowedModel(providerId = "changehero", expected = false),
        IsCexAllowedModel(providerId = "changelly", expected = true),
        IsCexAllowedModel(providerId = "simpleswap", expected = false),
    )
}