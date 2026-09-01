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

    /**
     * `isRestricted` is plumbed into [SwapQuoteUM.Content] but deliberately not read here: the provider
     * list renders a region-restricted quote exactly like a purchasable one, and the restriction only
     * surfaces as a blocking notification on Confirm (SwapNotificationsModelTest). The four tests below
     * pin that contract as pairs — same assertion, only the flag differs — so adding an indicator to
     * `getExtras` cannot land silently.
     */
    @Test
    fun `GIVEN restricted content quote WHEN convert THEN extra is empty`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.From)
        val restrictedQuote = contentQuote(isRestricted = true)

        // WHEN
        val item = converter.convert(restrictedQuote)

        // THEN
        assertThat(item).isNotNull()
        assertThat(item!!.providerUM.extraUM).isEqualTo(ProviderChooseUM.ExtraUM.Empty)
    }

    @Test
    fun `GIVEN non-restricted content quote WHEN convert THEN extra is empty`() {
        // GIVEN
        val converter = buildConverter(amountType = SwapAmountType.From)
        val contentQuote = contentQuote(isRestricted = false)

        // WHEN
        val item = converter.convert(contentQuote)

        // THEN
        assertThat(item).isNotNull()
        assertThat(item!!.providerUM.extraUM).isEqualTo(ProviderChooseUM.ExtraUM.Empty)
    }

    @Test
    fun `GIVEN restricted FCA provider WHEN convert THEN FCA warning action still shown`() {
        // GIVEN — the FCA branch is the only one guarding Content, so the restriction flag cannot pre-empt it
        val converter = buildConverter(amountType = SwapAmountType.From, isNeedApplyFCARestrictions = true)
        val restrictedQuote = contentQuote(isRestricted = true, provider = FCA_PROVIDER)

        // WHEN
        val item = converter.convert(restrictedQuote)

        // THEN
        assertThat(item).isNotNull()
        assertThat(item!!.providerUM.extraUM).isInstanceOf(ProviderChooseUM.ExtraUM.Action::class.java)
    }

    @Test
    fun `GIVEN purchasable FCA provider WHEN convert THEN FCA warning action shown`() {
        // GIVEN — same provider, only the restriction flag differs
        val converter = buildConverter(amountType = SwapAmountType.From, isNeedApplyFCARestrictions = true)
        val contentQuote = contentQuote(isRestricted = false, provider = FCA_PROVIDER)

        // WHEN
        val item = converter.convert(contentQuote)

        // THEN
        assertThat(item).isNotNull()
        assertThat(item!!.providerUM.extraUM).isInstanceOf(ProviderChooseUM.ExtraUM.Action::class.java)
    }

    private fun contentQuote(
        isRestricted: Boolean,
        provider: ExpressProvider = this.provider,
    ): SwapQuoteUM.Content = SwapQuoteUM.Content(
        provider = provider,
        toAmount = BigDecimal.ONE,
        fromAmount = BigDecimal.ONE,
        toAmountValue = TextReference.EMPTY,
        fromAmountValue = TextReference.EMPTY,
        diffPercent = SwapQuoteUM.Content.DifferencePercent.Empty,
        isSingleProvider = false,
        rate = TextReference.EMPTY,
        quoteId = null,
        isRestricted = isRestricted,
    )

    private fun buildConverter(
        amountType: SwapAmountType,
        isNeedApplyFCARestrictions: Boolean = false,
    ): SwapProviderListItemConverter {
        return SwapProviderListItemConverter(
            fromCryptoCurrency = fromCurrency,
            toCryptoCurrency = toCurrency,
            amountType = amountType,
            selectedProvider = provider,
            isNeedApplyFCARestrictions = isNeedApplyFCARestrictions,
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

        /** `providerId` must be one of `FCA_RESTRICTED_PROVIDER_IDS` for `isRestrictedByFCA()` to hold. */
        val FCA_PROVIDER = ExpressProvider(
            providerId = "changelly",
            name = "Changelly",
            type = ExpressProviderType.CEX,
            imageLarge = "",
            termsOfUse = null,
            privacyPolicy = null,
            slippage = null,
        )
    }
}