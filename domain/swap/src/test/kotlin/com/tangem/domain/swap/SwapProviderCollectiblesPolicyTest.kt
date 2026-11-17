package com.tangem.domain.swap

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SwapProviderCollectiblesPolicyTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN provider id WHEN isCexAllowed THEN returns expected`(model: IsCexAllowedModel) {
        // Act
        val actual = SwapProviderCollectiblesPolicy.isCexAllowed(providerId = model.providerId)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class IsCexAllowedModel(val providerId: String, val expected: Boolean)

    private fun provideTestModels() = listOf(
        IsCexAllowedModel(providerId = "changehero", expected = true),
        IsCexAllowedModel(providerId = "changelly", expected = false),
        IsCexAllowedModel(providerId = "simpleswap", expected = false),
    )
}