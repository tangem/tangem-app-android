package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SwapModelQuoteDefaultSlippageTest : SwapModelTestBase() {

    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val provider = swapProvider(id = "provider-1", slippage = PROVIDER_DEFAULT, isCustomSlippageSupported = true)
    private val otherProvider = swapProvider(id = "provider-2", slippage = PROVIDER_DEFAULT)

    private val from = pairCurrencyStatus(currencyId = "usdt")
    private val to = pairCurrencyStatus(currencyId = "usdc")
    private val quoteKey = SlippageQuoteKey(from, to, AMOUNT)

    @BeforeEach
    fun setUp() {
        setUpBase()
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
    }

    @Test
    fun `GIVEN no quote loaded WHEN default resolved THEN provider default`() {
        // Arrange
        val model = modelWithPair()

        // Act
        val actual = model.dataState.defaultSlippage(provider)

        // Assert
        assertThat(actual).isEqualTo(PROVIDER_DEFAULT)
    }

    @Test
    fun `GIVEN quote requested without custom WHEN remembered THEN quote slippage is the default`() {
        // Arrange
        val model = modelWithPair()

        // Act
        model.rememberQuoteDefaultSlippages(quoteKey, states = quotes(PAIR_DEFAULT))

        // Assert
        assertThat(model.dataState.defaultSlippage(provider)).isEqualTo(PAIR_DEFAULT)
    }

    @Test
    fun `GIVEN pair default remembered WHEN quote for custom slippage loaded THEN default kept`() {
        // Arrange
        val model = modelWithPair()
        model.rememberQuoteDefaultSlippages(quoteKey, states = quotes(PAIR_DEFAULT))
        val custom = BigDecimal("0.08")

        // Act
        model.rememberQuoteDefaultSlippages(quoteKey, states = quotes(custom, requestedCustomSlippage = custom))

        // Assert
        assertThat(model.dataState.defaultSlippage(provider)).isEqualTo(PAIR_DEFAULT)
    }

    @Test
    fun `GIVEN pair default remembered WHEN amount changed THEN provider default`() {
        // Arrange
        val model = modelWithPair()
        model.rememberQuoteDefaultSlippages(quoteKey, states = quotes(PAIR_DEFAULT))

        // Act
        model.dataState = model.dataState.copy(amount = "200")

        // Assert
        assertThat(model.dataState.defaultSlippage(provider)).isEqualTo(PROVIDER_DEFAULT)
    }

    @Test
    fun `GIVEN provider missing from next response WHEN remembered THEN its default kept`() {
        // Arrange
        val model = modelWithPair()
        model.rememberQuoteDefaultSlippages(quoteKey, states = quotes(PAIR_DEFAULT))
        val otherDefault = BigDecimal("0.3")

        // Act
        model.rememberQuoteDefaultSlippages(quoteKey, states = mapOf(otherProvider to quote(otherProvider, otherDefault)))

        // Assert
        assertThat(model.dataState.defaultSlippage(provider) to model.dataState.defaultSlippage(otherProvider))
            .isEqualTo(PAIR_DEFAULT to otherDefault)
    }

    @Test
    fun `GIVEN custom refresh WHEN provider quoted without custom THEN its default remembered`() {
        // Arrange
        val model = modelWithPair()
        val custom = BigDecimal("0.08")
        val otherDefault = BigDecimal("0.3")
        val states = mapOf(
            provider to quote(provider, custom, requestedCustomSlippage = custom),
            otherProvider to quote(otherProvider, otherDefault),
        )

        // Act
        model.rememberQuoteDefaultSlippages(quoteKey, states = states)

        // Assert
        assertThat(model.dataState.defaultSlippage(provider) to model.dataState.defaultSlippage(otherProvider))
            .isEqualTo(PROVIDER_DEFAULT to otherDefault)
    }

    @Test
    fun `GIVEN pair default remembered WHEN default confirmed THEN confirm event carries pair default`() {
        // Arrange
        val model = modelWithPair()
        model.rememberQuoteDefaultSlippages(quoteKey, states = quotes(PAIR_DEFAULT))

        // Act
        model.onSlippageConfirm(customSlippage = null)

        // Assert
        val event = sentEvents.filterIsInstance<SwapEvents.ButtonSlippageConfirm>().single()
        assertThat(event.params["Slippage"]).isEqualTo("0.05")
    }

    private fun modelWithPair(): SwapModel {
        val model = createModel()
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            amount = AMOUNT,
            selectedProvider = provider,
        )
        return model
    }

    private fun quotes(
        slippage: BigDecimal,
        requestedCustomSlippage: BigDecimal? = null,
    ): Map<SwapProvider, SwapState> = mapOf(provider to quote(provider, slippage, requestedCustomSlippage))

    private fun quote(
        provider: SwapProvider,
        slippage: BigDecimal,
        requestedCustomSlippage: BigDecimal? = null,
    ): SwapState.QuotesLoadedState = quotesLoadedState(provider).also {
        every { it.slippage } returns slippage
        every { it.requestedCustomSlippage } returns requestedCustomSlippage
    }

    private fun pairCurrencyStatus(currencyId: String): SwapCurrencyStatus {
        val accountId = mockk<AccountId>()
        val id = mockk<CryptoCurrency.ID>()
        return swapCurrencyStatus(
            currency = mockk<CryptoCurrency>(relaxed = true) {
                every { this@mockk.id } returns id
                every { symbol } returns currencyId
            },
            account = mockk<Account.CryptoPortfolio>(relaxed = true) {
                every { this@mockk.accountId } returns accountId
            },
        )
    }

    private companion object {
        const val AMOUNT = "100"
        val PROVIDER_DEFAULT = BigDecimal("2")
        val PAIR_DEFAULT = BigDecimal("0.05")
    }
}