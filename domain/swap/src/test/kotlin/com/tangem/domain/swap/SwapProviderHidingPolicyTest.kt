package com.tangem.domain.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExpressError
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SwapProviderHidingPolicyTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN provider and error WHEN shouldHide THEN returns expected`(model: ShouldHideModel) {
        // Act
        val actual = SwapProviderHidingPolicy.shouldHide(providerId = model.providerId, error = model.error)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class ShouldHideModel(val providerId: String, val error: ExpressError, val expected: Boolean)

    private val amlBanned = ExpressError.ExchangeProviderAddressBannedByAmlError(code = 2264)
    private val exchangeNotPossible = ExpressError.ExchangeNotPossibleError(code = 2240)
    private val dataError = ExpressError.DataError(code = 2252, description = null)

    private fun provideTestModels() = listOf(
        ShouldHideModel(providerId = "changehero", error = amlBanned, expected = true),
        ShouldHideModel(providerId = "changehero", error = exchangeNotPossible, expected = false),
        ShouldHideModel(providerId = "changehero", error = ExpressError.UnknownError(), expected = false),
        ShouldHideModel(providerId = "changelly", error = amlBanned, expected = true),
        ShouldHideModel(providerId = "changelly", error = exchangeNotPossible, expected = true),
        ShouldHideModel(providerId = "changelly", error = dataError, expected = true),
        ShouldHideModel(providerId = "changelly", error = ExpressError.UnknownError(), expected = true),
        ShouldHideModel(
            providerId = "changelly",
            error = ExpressError.AmountError.TooSmallError(code = 2250, amount = BigDecimal.ONE),
            expected = false,
        ),
        ShouldHideModel(
            providerId = "changelly",
            error = ExpressError.AmountError.TooBigError(code = 2251, amount = BigDecimal.TEN),
            expected = false,
        ),
        ShouldHideModel(
            providerId = "changelly",
            error = ExpressError.NotEnoughBalanceError(code = 2270),
            expected = false,
        ),
        ShouldHideModel(
            providerId = "changelly",
            error = ExpressError.InvalidAddressError(code = 2280),
            expected = false,
        ),
        ShouldHideModel(
            providerId = "changelly",
            error = ExpressError.ProviderDifferentAmountError(
                code = 2320,
                fromAmount = BigDecimal.ONE,
                fromProviderAmount = BigDecimal.TEN,
                decimals = 18,
            ),
            expected = false,
        ),
        ShouldHideModel(providerId = "swapkit", error = amlBanned, expected = false),
        ShouldHideModel(providerId = "changenow", error = ExpressError.UnknownError(), expected = false),
    )
}