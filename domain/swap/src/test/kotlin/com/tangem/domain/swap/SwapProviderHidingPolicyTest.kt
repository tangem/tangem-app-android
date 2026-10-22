package com.tangem.domain.swap

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SwapProviderHidingPolicyTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN provider and error code WHEN shouldHide THEN returns expected`(model: ShouldHideModel) {
        // Act
        val actual = SwapProviderHidingPolicy.shouldHide(providerId = model.providerId, errorCode = model.errorCode)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class ShouldHideModel(val providerId: String, val errorCode: Int, val expected: Boolean)

    private fun provideTestModels() = listOf(
        ShouldHideModel(providerId = "changehero", errorCode = 2264, expected = true),
        ShouldHideModel(providerId = "changehero", errorCode = 2240, expected = false),
        ShouldHideModel(providerId = "changehero", errorCode = -1, expected = false),
        ShouldHideModel(providerId = "changelly", errorCode = 2264, expected = true),
        ShouldHideModel(providerId = "changelly", errorCode = 2240, expected = true),
        ShouldHideModel(providerId = "changelly", errorCode = -1, expected = true),
        ShouldHideModel(providerId = "changelly", errorCode = 2250, expected = false),
        ShouldHideModel(providerId = "changelly", errorCode = 2251, expected = false),
        ShouldHideModel(providerId = "changelly", errorCode = 2252, expected = false),
        ShouldHideModel(providerId = "changelly", errorCode = 2270, expected = false),
        ShouldHideModel(providerId = "changelly", errorCode = 2280, expected = false),
        ShouldHideModel(providerId = "changelly", errorCode = 2310, expected = false),
        ShouldHideModel(providerId = "changelly", errorCode = 2320, expected = false),
        ShouldHideModel(providerId = "swapkit", errorCode = 2264, expected = false),
        ShouldHideModel(providerId = "changenow", errorCode = -1, expected = false),
    )
}