package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressError
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
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import java.math.BigDecimal

internal class SwapAmountSelectQuoteTransformerTest {

    private val provider = ExpressProvider(
        providerId = "test-provider",
        name = "Test Provider",
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    @Test
    fun `GIVEN fixed mode quote with fromAmount exceeding primary balance WHEN transform THEN isPrimaryButtonEnabled is false`() {
        // GIVEN
        val prevState = buildContentState(
            selectedAmountType = SwapAmountType.To,
            primaryBalance = BigDecimal("10"),
        )
        val quote = buildContentQuote(fromAmount = BigDecimal("20"), toAmount = BigDecimal("1"))
        val transformer = buildTransformer(quoteUM = quote)

        // WHEN
        val result = transformer.transform(prevState)

        // THEN
        val content = result as SwapAmountUM.Content
        assertThat(content.isPrimaryButtonEnabled).isFalse()
        assertThat(content.selectedQuote).isEqualTo(quote)
    }

    @Test
    fun `GIVEN fixed mode quote with fromAmount within primary balance WHEN transform THEN isPrimaryButtonEnabled is true`() {
        // GIVEN
        val prevState = buildContentState(
            selectedAmountType = SwapAmountType.To,
            primaryBalance = BigDecimal("100"),
        )
        val quote = buildContentQuote(fromAmount = BigDecimal("20"), toAmount = BigDecimal("1"))
        val transformer = buildTransformer(quoteUM = quote)

        // WHEN
        val result = transformer.transform(prevState)

        // THEN
        val content = result as SwapAmountUM.Content
        assertThat(content.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN float mode with selectedAmountType From WHEN transform THEN isPrimaryButtonEnabled is true regardless of fromAmount`() {
        // GIVEN
        val prevState = buildContentState(
            selectedAmountType = SwapAmountType.From,
            primaryBalance = BigDecimal("10"),
        )
        // fromAmount > balance, but we're in From-mode so the check must not fire
        val quote = buildContentQuote(fromAmount = BigDecimal("20"), toAmount = BigDecimal("1"))
        val transformer = buildTransformer(quoteUM = quote)

        // WHEN
        val result = transformer.transform(prevState)

        // THEN
        val content = result as SwapAmountUM.Content
        assertThat(content.isPrimaryButtonEnabled).isTrue()
    }

    @Test
    fun `GIVEN quote is SwapQuoteUM Error WHEN transform THEN isPrimaryButtonEnabled is false`() {
        // GIVEN
        val prevState = buildContentState(
            selectedAmountType = SwapAmountType.To,
            primaryBalance = BigDecimal("100"),
        )
        val errorQuote = SwapQuoteUM.Error(
            provider = provider,
            expressError = ExpressError.InternalError(code = 500),
        )
        val transformer = buildTransformer(quoteUM = errorQuote)

        // WHEN
        val result = transformer.transform(prevState)

        // THEN
        val content = result as SwapAmountUM.Content
        assertThat(content.isPrimaryButtonEnabled).isFalse()
        assertThat(content.selectedQuote).isEqualTo(errorQuote)
    }

    @Test
    fun `GIVEN prevState is SwapAmountUM Empty WHEN transform THEN returns the same state`() {
        // GIVEN
        val prevState = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct)
        val quote = buildContentQuote(fromAmount = BigDecimal("20"), toAmount = BigDecimal("1"))
        val transformer = buildTransformer(quoteUM = quote)

        // WHEN
        val result = transformer.transform(prevState)

        // THEN
        assertThat(result).isEqualTo(prevState)
    }

    private fun buildTransformer(quoteUM: SwapQuoteUM): SwapAmountSelectQuoteTransformer {
        return SwapAmountSelectQuoteTransformer(
            quoteUM = quoteUM,
            secondaryMaximumAmountBoundary = null,
            secondaryMinimumAmountBoundary = null,
            isNeedApplyFCARestrictions = false,
            isBalanceHidden = false,
            primaryMaximumAmountBoundary = null,
            primaryMinimumAmountBoundary = null,
            primaryFiatRateUSD = null,
            secondaryFiatRateUSD = null,
        )
    }

    private fun buildContentQuote(fromAmount: BigDecimal, toAmount: BigDecimal): SwapQuoteUM.Content {
        return SwapQuoteUM.Content(
            provider = provider,
            toAmount = toAmount,
            fromAmount = fromAmount,
            toAmountValue = TextReference.EMPTY,
            fromAmountValue = TextReference.EMPTY,
            diffPercent = SwapQuoteUM.Content.DifferencePercent.Empty,
            isSingleProvider = true,
            rate = TextReference.EMPTY,
            quoteId = null,
        )
    }

    private fun buildContentState(
        selectedAmountType: SwapAmountType,
        primaryBalance: BigDecimal?,
    ): SwapAmountUM.Content {
        val primaryStatus = mockk<CryptoCurrencyStatus>(relaxed = true).also { status ->
            every { status.value.amount } returns primaryBalance
            every { status.value.fiatRate } returns null
            every { status.value.fiatAmount } returns null
            every { status.currency.symbol } returns "BTC"
            every { status.currency.decimals } returns 8
        }
        return SwapAmountUM.Content(
            isPrimaryButtonEnabled = false,
            swapDirection = SwapDirection.Direct,
            selectedAmountType = selectedAmountType,
            primaryAmount = SwapAmountFieldUM.Empty(SwapAmountType.From),
            secondaryAmount = SwapAmountFieldUM.Empty(SwapAmountType.To),
            primaryCryptoCurrencyStatus = primaryStatus,
            secondaryCryptoCurrencyStatus = null,
            swapRateType = ExpressRateType.Fixed,
            swapRateMode = SwapRateMode.FIXED_ONLY,
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