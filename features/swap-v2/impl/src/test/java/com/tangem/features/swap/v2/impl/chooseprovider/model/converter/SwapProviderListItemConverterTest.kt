package com.tangem.features.swap.v2.impl.chooseprovider.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.provider.entity.ProviderChooseUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SwapProviderListItemConverterTest {

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
    fun `GIVEN quote with TooSmallError and amountType To WHEN convert THEN error text uses to symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.To)
        val errorQuote = errorQuote(ExpressError.AmountError.TooSmallError(code = 1, amount = BigDecimal("1.5")))

        // WHEN
        val item = converter.convert(errorQuote)

        // THEN
        assertThat(item).isNotNull()
        val errorText = (item!!.providerUM.extraUM as ProviderChooseUM.ExtraUM.Error).text
        assertSymbolUsed(errorText, expectedSymbol = TO_SYMBOL, otherSymbol = FROM_SYMBOL)
    }

    @Test
    fun `GIVEN quote with TooSmallError and amountType From WHEN convert THEN error text uses from symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.From)
        val errorQuote = errorQuote(ExpressError.AmountError.TooSmallError(code = 1, amount = BigDecimal("1.5")))

        // WHEN
        val item = converter.convert(errorQuote)

        // THEN
        assertThat(item).isNotNull()
        val errorText = (item!!.providerUM.extraUM as ProviderChooseUM.ExtraUM.Error).text
        assertSymbolUsed(errorText, expectedSymbol = FROM_SYMBOL, otherSymbol = TO_SYMBOL)
    }

    @Test
    fun `GIVEN quote with TooBigError and amountType To WHEN convert THEN error text uses to symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.To)
        val errorQuote = errorQuote(ExpressError.AmountError.TooBigError(code = 2, amount = BigDecimal("999")))

        // WHEN
        val item = converter.convert(errorQuote)

        // THEN
        assertThat(item).isNotNull()
        val errorText = (item!!.providerUM.extraUM as ProviderChooseUM.ExtraUM.Error).text
        assertSymbolUsed(errorText, expectedSymbol = TO_SYMBOL, otherSymbol = FROM_SYMBOL)
    }

    @Test
    fun `GIVEN quote with TooBigError and amountType From WHEN convert THEN error text uses from symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.From)
        val errorQuote = errorQuote(ExpressError.AmountError.TooBigError(code = 2, amount = BigDecimal("999")))

        // WHEN
        val item = converter.convert(errorQuote)

        // THEN
        assertThat(item).isNotNull()
        val errorText = (item!!.providerUM.extraUM as ProviderChooseUM.ExtraUM.Error).text
        assertSymbolUsed(errorText, expectedSymbol = FROM_SYMBOL, otherSymbol = TO_SYMBOL)
    }

    @Test
    fun `GIVEN quote with NotEnoughAllowanceError and amountType From WHEN convert THEN error text uses from symbol`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.From)
        val errorQuote = errorQuote(
            ExpressError.AmountError.NotEnoughAllowanceError(code = 3, amount = BigDecimal("10")),
        )

        // WHEN
        val item = converter.convert(errorQuote)

        // THEN
        assertThat(item).isNotNull()
        val errorText = (item!!.providerUM.extraUM as ProviderChooseUM.ExtraUM.Error).text
        assertSymbolUsed(errorText, expectedSymbol = FROM_SYMBOL, otherSymbol = TO_SYMBOL)
    }

    private fun buildConverter(amountType: SwapAmountType): SwapProviderListItemConverter {
        return SwapProviderListItemConverter(
            fromCryptoCurrency = fromCurrency,
            toCryptoCurrency = toCurrency,
            amountType = amountType,
            selectedProvider = provider,
            isNeedApplyFCARestrictions = false,
            needBestRateBadge = false,
        )
    }

    private fun errorQuote(error: ExpressError): SwapQuoteUM.Error = SwapQuoteUM.Error(
        provider = provider,
        expressError = error,
    )

    private fun assertSymbolUsed(text: TextReference, expectedSymbol: String, otherSymbol: String) {
        val res = text as TextReference.Res
        val formatted = res.formatArgs.first().toString()
        assertThat(formatted).contains(expectedSymbol)
        assertThat(formatted).doesNotContain(otherSymbol)
    }

    private companion object {
        const val FROM_SYMBOL = "ETH"
        const val TO_SYMBOL = "BTC"
    }
}