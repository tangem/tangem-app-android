package com.tangem.features.swap.v2.impl.chooseprovider.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.audits.AuditLabelUM
import com.tangem.core.ui.components.provider.entity.ProviderChooseUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.features.swap.v2.impl.R
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
     * A region-restricted quote is marked by its label, not by `getExtras`: the extra slot stays whatever
     * the quote itself calls for (empty, or the FCA warning action). The four tests below pin that as
     * pairs — same assertion, only the restriction flag differs.
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

    @Test
    fun `GIVEN restricted content quote WHEN convert THEN label says the provider is not available`() {
        // Arrange
        val converter = buildConverter(amountType = SwapAmountType.From)
        val restrictedQuote = contentQuote(isRestricted = true)

        // Act
        val item = converter.convert(restrictedQuote)

        // Assert
        assertThat(item).isNotNull()
        val label = item!!.providerUM.labelUM as ProviderChooseUM.LabelUM.Info
        assertThat(label.auditLabelUM.type).isEqualTo(AuditLabelUM.Type.General)
        assertThat((label.auditLabelUM.text as TextReference.Res).id).isEqualTo(R.string.common_not_available)
    }

    @Test
    fun `GIVEN restricted content quote with rate difference WHEN convert THEN difference is replaced`() {
        // Arrange
        val converter = buildConverter(amountType = SwapAmountType.From)
        val restrictedQuote = contentQuote(
            isRestricted = true,
            diffPercent = SwapQuoteUM.Content.DifferencePercent.Diff(
                isPositive = true,
                percent = stringReference("+0.09%"),
            ),
        )

        // Act
        val item = converter.convert(restrictedQuote)

        // Assert
        assertThat(item).isNotNull()
        val label = item!!.providerUM.labelUM as ProviderChooseUM.LabelUM.Info
        assertThat((label.auditLabelUM.text as TextReference.Res).id).isEqualTo(R.string.common_not_available)
    }

    @Test
    fun `GIVEN restricted best rate quote WHEN convert THEN not available wins over best rate`() {
        // Arrange
        val converter = buildConverter(amountType = SwapAmountType.From)
        val restrictedQuote = contentQuote(
            isRestricted = true,
            diffPercent = SwapQuoteUM.Content.DifferencePercent.Best,
        )

        // Act
        val item = converter.convert(restrictedQuote)

        // Assert
        assertThat(item).isNotNull()
        val label = item!!.providerUM.labelUM as ProviderChooseUM.LabelUM.Info
        assertThat((label.auditLabelUM.text as TextReference.Res).id).isEqualTo(R.string.common_not_available)
    }

    @Test
    fun `GIVEN purchasable best rate quote WHEN convert THEN label says best rate`() {
        // Arrange
        val converter = buildConverter(amountType = SwapAmountType.From)
        val contentQuote = contentQuote(
            isRestricted = false,
            diffPercent = SwapQuoteUM.Content.DifferencePercent.Best,
        )

        // Act
        val item = converter.convert(contentQuote)

        // Assert
        assertThat(item).isNotNull()
        val label = item!!.providerUM.labelUM as ProviderChooseUM.LabelUM.Info
        assertThat((label.auditLabelUM.text as TextReference.Res).id).isEqualTo(R.string.express_provider_best_rate)
    }

    private fun contentQuote(
        isRestricted: Boolean,
        provider: ExpressProvider = this.provider,
        diffPercent: SwapQuoteUM.Content.DifferencePercent = SwapQuoteUM.Content.DifferencePercent.Empty,
    ): SwapQuoteUM.Content = SwapQuoteUM.Content(
        provider = provider,
        toAmount = BigDecimal.ONE,
        fromAmount = BigDecimal.ONE,
        toAmountValue = TextReference.EMPTY,
        fromAmountValue = TextReference.EMPTY,
        diffPercent = diffPercent,
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