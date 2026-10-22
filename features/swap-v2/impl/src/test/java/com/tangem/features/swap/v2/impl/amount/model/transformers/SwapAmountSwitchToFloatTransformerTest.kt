package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.SwapRateMode
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class SwapAmountSwitchToFloatTransformerTest {

    private val transformer = SwapAmountSwitchToFloatTransformer(isBalanceHidden = false)

    @Test
    fun `GIVEN empty state WHEN transform THEN state is unchanged`() {
        // Arrange
        val state = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct)

        // Act
        val result = transformer.transform(state)

        // Assert
        assertThat(result).isEqualTo(state)
    }

    @Test
    fun `GIVEN receive amount entered at fixed rate WHEN transform THEN input moves to sent amount at float rate`() {
        // Arrange
        val state = buildContentState()

        // Act
        val result = transformer.transform(state)

        // Assert
        val expected = state.copy(
            selectedAmountType = SwapAmountType.From,
            swapRateType = ExpressRateType.Float,
            swapRateMode = SwapRateMode.FLOAT_ONLY,
        )
        assertThat(result).isEqualTo(expected)
    }

    private fun buildContentState() = SwapAmountUM.Content(
        isPrimaryButtonEnabled = true,
        swapDirection = SwapDirection.Direct,
        selectedAmountType = SwapAmountType.To,
        primaryAmount = SwapAmountFieldUM.Empty(SwapAmountType.From),
        secondaryAmount = SwapAmountFieldUM.Empty(SwapAmountType.To),
        primaryCryptoCurrencyStatus = mockk<CryptoCurrencyStatus>(relaxed = true),
        secondaryCryptoCurrencyStatus = null,
        swapRateType = ExpressRateType.Fixed,
        swapRateMode = SwapRateMode.FLOAT_AND_FIXED,
        priceImpact = null,
        swapCurrencies = SwapCurrencies.EMPTY,
        swapQuotes = persistentListOf(),
        selectedQuote = SwapQuoteUM.Empty,
        isShowFCAWarning = false,
        appCurrency = null,
        isShowBestRateAnimation = false,
    )
}