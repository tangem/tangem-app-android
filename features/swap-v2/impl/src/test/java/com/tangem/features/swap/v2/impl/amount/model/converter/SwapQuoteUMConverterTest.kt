package com.tangem.features.swap.v2.impl.amount.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.SwapQuoteModel
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * `SwapQuoteUM.Content.isRestricted` is the single value every downstream swap-v2 consumer reads — the
 * provider-row error and the Confirm-screen Send gate. If this converter drops the flag, a restricted
 * token becomes sendable, so both `Content` construction sites are pinned here.
 */
internal class SwapQuoteUMConverterTest {

    private val fromCurrency = mockk<CryptoCurrency>(relaxed = true).also {
        every { it.symbol } returns "ETH"
        every { it.decimals } returns 18
    }
    private val toCurrency = mockk<CryptoCurrency>(relaxed = true).also {
        every { it.symbol } returns "BTC"
        every { it.decimals } returns 8
    }

    @Test
    fun `GIVEN restricted quote without allowance WHEN convert THEN content keeps the restriction flag`() {
        // Arrange
        val converter = buildConverter(allowanceContract = null, isApprovalNeeded = false)

        // Act
        val result = converter.convert(buildData(isRestricted = true))

        // Assert
        assertThat((result as SwapQuoteUM.Content).isRestricted).isTrue()
    }

    @Test
    fun `GIVEN restricted quote with approved allowance WHEN convert THEN content keeps the restriction flag`() {
        // Arrange
        val converter = buildConverter(allowanceContract = "0xSpender", isApprovalNeeded = false)

        // Act
        val result = converter.convert(buildData(isRestricted = true))

        // Assert
        assertThat((result as SwapQuoteUM.Content).isRestricted).isTrue()
    }

    @Test
    fun `GIVEN purchasable quote WHEN convert THEN content is not restricted`() {
        // Arrange
        val converter = buildConverter(allowanceContract = null, isApprovalNeeded = false)

        // Act
        val result = converter.convert(buildData(isRestricted = false))

        // Assert
        assertThat((result as SwapQuoteUM.Content).isRestricted).isFalse()
    }

    /**
     * `SwapQuoteUM.Allowance` has no `isRestricted` field, so the flag is structurally lost when an
     * approval is still pending. Harmless while Send-with-Swap is CEX-only (no allowance step), but the
     * loss is deliberate rather than overlooked — this test records that.
     */
    @Test
    fun `GIVEN restricted quote needing approval WHEN convert THEN allowance state carries no flag`() {
        // Arrange
        val converter = buildConverter(allowanceContract = "0xSpender", isApprovalNeeded = true)

        // Act
        val result = converter.convert(buildData(isRestricted = true))

        // Assert
        assertThat(result).isInstanceOf(SwapQuoteUM.Allowance::class.java)
    }

    private fun buildConverter(allowanceContract: String?, isApprovalNeeded: Boolean) = SwapQuoteUMConverter(
        swapDirection = SwapDirection.Direct,
        allowanceContract = allowanceContract,
        isApprovalNeeded = isApprovalNeeded,
        primaryCurrency = fromCurrency,
        secondaryCurrency = toCurrency,
        fromAmount = BigDecimal.ONE,
    )

    private fun buildData(isRestricted: Boolean) = SwapQuoteUMConverter.Data(
        quote = SwapQuoteModel(
            provider = PROVIDER,
            toTokenAmount = BigDecimal("0.5"),
            fromTokenAmount = BigDecimal.ONE,
            allowanceContract = null,
            isRestricted = isRestricted,
        ),
        provider = PROVIDER,
    )

    private companion object {
        val PROVIDER = ExpressProvider(
            providerId = "p1",
            name = "Test Provider",
            type = ExpressProviderType.CEX,
            imageLarge = "",
            termsOfUse = null,
            privacyPolicy = null,
            slippage = null,
        )
    }
}