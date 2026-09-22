package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.SwapRateMode
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM.Content.DifferencePercent
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SwapAmountSetQuotesTransformerTest {

    private val availableProvider = provider(id = "available")
    private val restrictedProvider = provider(id = "restricted")

    @Test
    fun `GIVEN top rate quote is region restricted WHEN transform THEN best available quote is selected`() {
        // Arrange
        val restrictedQuote = quote(provider = restrictedProvider, toAmount = "120", isRestricted = true)
        val availableQuote = quote(provider = availableProvider, toAmount = "110")
        val transformer = buildTransformer(quotes = listOf(restrictedQuote, availableQuote))

        // Act
        val result = transformer.transform(buildContentState()) as SwapAmountUM.Content

        // Assert
        assertThat(result.selectedQuote.provider).isEqualTo(availableProvider)
    }

    @Test
    fun `GIVEN top rate quote is region restricted WHEN transform THEN best rate marks the available quote`() {
        // Arrange
        val restrictedQuote = quote(provider = restrictedProvider, toAmount = "120", isRestricted = true)
        val availableQuote = quote(provider = availableProvider, toAmount = "110")
        val transformer = buildTransformer(quotes = listOf(restrictedQuote, availableQuote))

        // Act
        val result = transformer.transform(buildContentState()) as SwapAmountUM.Content

        // Assert
        val diffByProvider = result.swapQuotes
            .filterIsInstance<SwapQuoteUM.Content>()
            .associate { it.provider to it.diffPercent }
        assertThat(diffByProvider[availableProvider]).isEqualTo(DifferencePercent.Best)
        assertThat(diffByProvider[restrictedProvider]).isInstanceOf(DifferencePercent.Diff::class.java)
    }

    @Test
    fun `GIVEN every quote is region restricted WHEN transform THEN top rate quote is selected`() {
        // Arrange
        val topQuote = quote(provider = restrictedProvider, toAmount = "120", isRestricted = true)
        val lowerQuote = quote(provider = availableProvider, toAmount = "110", isRestricted = true)
        val transformer = buildTransformer(quotes = listOf(topQuote, lowerQuote))

        // Act
        val result = transformer.transform(buildContentState()) as SwapAmountUM.Content

        // Assert
        assertThat(result.selectedQuote.provider).isEqualTo(restrictedProvider)
    }

    private fun buildTransformer(quotes: List<SwapQuoteUM>) = SwapAmountSetQuotesTransformer(
        quotes = quotes,
        secondaryMaximumAmountBoundary = null,
        secondaryMinimumAmountBoundary = null,
        isSilentReload = false,
        isNeedApplyFcaRestrictions = false,
        isBalanceHidden = false,
        primaryMaximumAmountBoundary = null,
        primaryMinimumAmountBoundary = null,
        primaryFiatRateUSD = null,
        secondaryFiatRateUSD = null,
    )

    private fun provider(id: String) = ExpressProvider(
        providerId = id,
        name = id,
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    private fun quote(
        provider: ExpressProvider,
        toAmount: String,
        isRestricted: Boolean = false,
    ) = SwapQuoteUM.Content(
        provider = provider,
        toAmount = BigDecimal(toAmount),
        fromAmount = null,
        toAmountValue = TextReference.EMPTY,
        fromAmountValue = TextReference.EMPTY,
        diffPercent = DifferencePercent.Empty,
        isSingleProvider = false,
        rate = TextReference.EMPTY,
        quoteId = null,
        isRestricted = isRestricted,
    )

    private fun buildContentState(): SwapAmountUM.Content {
        val primaryStatus = mockk<CryptoCurrencyStatus>(relaxed = true).also { status ->
            every { status.value.amount } returns BigDecimal("1000")
            every { status.value.fiatRate } returns null
            every { status.value.fiatAmount } returns null
            every { status.currency.symbol } returns "BTC"
            every { status.currency.decimals } returns 8
        }
        return SwapAmountUM.Content(
            isPrimaryButtonEnabled = true,
            swapDirection = SwapDirection.Direct,
            selectedAmountType = SwapAmountType.From,
            primaryAmount = SwapAmountFieldUM.Empty(SwapAmountType.From),
            secondaryAmount = SwapAmountFieldUM.Empty(SwapAmountType.To),
            primaryCryptoCurrencyStatus = primaryStatus,
            secondaryCryptoCurrencyStatus = null,
            swapRateType = ExpressRateType.Float,
            swapRateMode = SwapRateMode.FLOAT_ONLY,
            priceImpact = null,
            swapCurrencies = SwapCurrencies.EMPTY,
            swapQuotes = persistentListOf(),
            selectedQuote = SwapQuoteUM.Empty,
            isShowFCAWarning = false,
            appCurrency = null,
            isShowBestRateAnimation = false,
        )
    }
}