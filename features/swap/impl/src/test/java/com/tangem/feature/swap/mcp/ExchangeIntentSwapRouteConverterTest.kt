package com.tangem.feature.swap.mcp

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExchangeIntentSwapRouteConverterTest {

    private val converter = ExchangeIntentSwapRouteConverter(amountConverter = ExchangeIntentAmountConverter())

    private val walletId = UserWalletId("011")
    private val from = createToken()
    private val to = createCoin(network = createNetwork(rawId = "polygon-pos", name = "Polygon"), symbol = "POL")

    @Test
    fun `GIVEN intent WHEN convert THEN swap gets pair, amount and provider and is bound to the intent`() {
        // Arrange
        val intent = createExchangeIntent(id = "intent-1", providerId = "changelly")

        // Act
        val actual = convert(intent)

        // Assert
        val expected = AppRoute.Swap(
            userWalletId = walletId,
            fromCryptoCurrency = from,
            toCryptoCurrency = to,
            screenSource = AnalyticsParam.ScreensSources.Main.value,
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
            fromAmount = null,
            providerId = "changelly",
            intentId = "intent-1",
        )
        assertThat(actual.copy(fromAmount = null)).isEqualTo(expected)
        assertThat(actual.fromAmount?.toPlainString()).isEqualTo("42")
    }

    @Test
    fun `GIVEN currency with other decimals than the intent asset WHEN convert THEN amount follows the intent asset`() {
        // Arrange
        val intent = createExchangeIntent(amountType = SwapAmountType.From, amount = "42000000")
        val currencyWithOtherDecimals = createCoin()

        // Act
        val actual = converter.convert(
            ExchangeIntentSwapRouteConverter.Input(
                intent = intent,
                userWalletId = walletId,
                from = currencyWithOtherDecimals,
                to = to,
            ),
        )

        // Assert
        assertThat(currencyWithOtherDecimals.decimals).isNotEqualTo(intent.from.decimals)
        assertThat(actual.fromAmount?.toPlainString()).isEqualTo("42")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `from amount of swap route`(model: AmountModel) {
        // Arrange
        val intent = createExchangeIntent(amountType = model.side, amount = model.amount)

        // Act
        val actual = convert(intent)

        // Assert
        assertThat(actual.fromAmount?.toPlainString()).isEqualTo(model.expected)
    }

    private fun convert(intent: ExchangeIntent): AppRoute.Swap = converter.convert(
        ExchangeIntentSwapRouteConverter.Input(intent = intent, userWalletId = walletId, from = from, to = to),
    )

    data class AmountModel(val side: SwapAmountType, val amount: String, val expected: String?)

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        AmountModel(SwapAmountType.From, amount = "42000000", expected = "42"),
        AmountModel(SwapAmountType.From, amount = "1500000", expected = "1.5"),
        AmountModel(SwapAmountType.From, amount = "100000000", expected = "100"),
        AmountModel(SwapAmountType.From, amount = "1", expected = "0.000001"),
        AmountModel(SwapAmountType.From, amount = "0", expected = null),
        AmountModel(SwapAmountType.From, amount = "-5", expected = null),
        AmountModel(SwapAmountType.From, amount = "forty two", expected = null),
        AmountModel(SwapAmountType.To, amount = "42000000", expected = null),
    )
}