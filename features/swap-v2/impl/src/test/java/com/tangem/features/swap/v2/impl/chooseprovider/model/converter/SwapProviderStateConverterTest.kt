package com.tangem.features.swap.v2.impl.chooseprovider.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.features.swap.v2.impl.chooseprovider.entity.SwapProviderState
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@Suppress("DEPRECATION")
internal class SwapProviderStateConverterTest {

    private val provider = ExpressProvider(
        providerId = "p1",
        name = "Test Provider",
        type = ExpressProviderType.CEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    private val fromCurrency = mockk<CryptoCurrency>(relaxed = true).also {
        every { it.symbol } returns FROM_SYMBOL
        every { it.decimals } returns 18
    }

    private val toCurrency = mockk<CryptoCurrency>(relaxed = true).also {
        every { it.symbol } returns TO_SYMBOL
        every { it.decimals } returns 8
    }

    @Test
    fun `GIVEN error quote TooSmallError and amountType To WHEN convert THEN subtitle uses to symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.To)
        val errorQuote = errorQuote(ExpressError.AmountError.TooSmallError(code = 1, amount = BigDecimal("1.5")))

        // WHEN
        val state = converter.convert(errorQuote)

        // THEN
        assertSymbolUsed(state, expectedSymbol = TO_SYMBOL, otherSymbol = FROM_SYMBOL)
    }

    @Test
    fun `GIVEN error quote TooSmallError and amountType From WHEN convert THEN subtitle uses from symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.From)
        val errorQuote = errorQuote(ExpressError.AmountError.TooSmallError(code = 1, amount = BigDecimal("1.5")))

        // WHEN
        val state = converter.convert(errorQuote)

        // THEN
        assertSymbolUsed(state, expectedSymbol = FROM_SYMBOL, otherSymbol = TO_SYMBOL)
    }

    @Test
    fun `GIVEN error quote TooBigError and amountType To WHEN convert THEN subtitle uses to symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.To)
        val errorQuote = errorQuote(ExpressError.AmountError.TooBigError(code = 2, amount = BigDecimal("999")))

        // WHEN
        val state = converter.convert(errorQuote)

        // THEN
        assertSymbolUsed(state, expectedSymbol = TO_SYMBOL, otherSymbol = FROM_SYMBOL)
    }

    @Test
    fun `GIVEN error quote TooBigError and amountType From WHEN convert THEN subtitle uses from symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.From)
        val errorQuote = errorQuote(ExpressError.AmountError.TooBigError(code = 2, amount = BigDecimal("999")))

        // WHEN
        val state = converter.convert(errorQuote)

        // THEN
        assertSymbolUsed(state, expectedSymbol = FROM_SYMBOL, otherSymbol = TO_SYMBOL)
    }

    @Test
    fun `GIVEN error quote NotEnoughAllowanceError and amountType From WHEN convert THEN subtitle uses from symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.From)
        val errorQuote = errorQuote(
            ExpressError.AmountError.NotEnoughAllowanceError(code = 3, amount = BigDecimal("10")),
        )

        // WHEN
        val state = converter.convert(errorQuote)

        // THEN
        assertSymbolUsed(state, expectedSymbol = FROM_SYMBOL, otherSymbol = TO_SYMBOL)
    }

    private fun buildConverter(amountType: SwapAmountType): SwapProviderStateConverter {
        return SwapProviderStateConverter(
            fromCryptoCurrency = fromCurrency,
            toCryptoCurrency = toCurrency,
            amountType = amountType,
            selectedProvider = provider,
            isNeedBestRateBadge = false,
            isNeedApplyFCARestrictions = false,
        )
    }

    private fun errorQuote(error: ExpressError): SwapQuoteUM.Error = SwapQuoteUM.Error(
        provider = provider,
        expressError = error,
    )

    private fun assertSymbolUsed(state: SwapProviderState, expectedSymbol: String, otherSymbol: String) {
        val content = state as SwapProviderState.Content
        val subtitle = content.subtitle as TextReference.Res
        val formatted = subtitle.formatArgs.first().toString()
        assertThat(formatted).contains(expectedSymbol)
        assertThat(formatted).doesNotContain(otherSymbol)
    }

    private companion object {
        const val FROM_SYMBOL = "ETH"
        const val TO_SYMBOL = "BTC"
    }
}