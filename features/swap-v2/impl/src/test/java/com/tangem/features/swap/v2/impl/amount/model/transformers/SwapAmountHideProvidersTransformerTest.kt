package com.tangem.features.swap.v2.impl.amount.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapCryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapCurrenciesGroup
import com.tangem.domain.swap.models.SwapDirection
import com.tangem.domain.swap.models.SwapRateMode
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM.Content.DifferencePercent
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SwapAmountHideProvidersTransformerTest {

    private val changelly = provider(id = "changelly")
    private val changeHero = provider(id = "changehero")
    private val simpleSwap = provider(id = "simpleswap")

    private val currencyStatus: CryptoCurrencyStatus = mockk(relaxed = true)

    @Test
    fun `GIVEN empty state WHEN transform THEN state is unchanged`() {
        // Arrange
        val state = SwapAmountUM.Empty(swapDirection = SwapDirection.Direct)

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        assertThat(result).isEqualTo(state)
    }

    @Test
    fun `GIVEN hidden provider is not selected WHEN transform THEN it is dropped and selection is kept`() {
        // Arrange
        val changeHeroQuote = quote(provider = changeHero)
        val state = buildContentState(
            providers = listOf(changelly, changeHero),
            quotes = listOf(changeHeroQuote, quote(provider = changelly)),
            selectedQuote = changeHeroQuote,
        )

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        val expected = state.copy(
            swapCurrencies = swapCurrencies(providers = listOf(changeHero)),
            swapQuotes = persistentListOf(changeHeroQuote),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN hidden provider is selected and others are left WHEN transform THEN best remaining quote is selected`() {
        // Arrange
        val changellyQuote = quote(provider = changelly)
        val changeHeroQuote = quote(provider = changeHero)
        val simpleSwapQuote = quote(provider = simpleSwap)
        val state = buildContentState(
            providers = listOf(changelly, changeHero, simpleSwap),
            quotes = listOf(changellyQuote, changeHeroQuote, simpleSwapQuote),
            selectedQuote = changellyQuote,
        )

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        result as SwapAmountUM.Content
        assertThat(result.selectedQuote).isEqualTo(changeHeroQuote)
        assertThat(result.swapQuotes).containsExactly(changeHeroQuote, simpleSwapQuote).inOrder()
    }

    @Test
    fun `GIVEN only error quotes are left WHEN transform THEN quote is empty`() {
        // Arrange
        val changellyQuote = quote(provider = changelly)
        val changeHeroError = SwapQuoteUM.Error(provider = changeHero, expressError = mockk(relaxed = true))
        val state = buildContentState(
            providers = listOf(changelly, changeHero),
            quotes = listOf(changellyQuote, changeHeroError),
            selectedQuote = changellyQuote,
        )

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        result as SwapAmountUM.Content
        assertThat(result.selectedQuote).isEqualTo(SwapQuoteUM.Empty)
        assertThat(result.swapQuotes).containsExactly(changeHeroError)
    }

    @Test
    fun `GIVEN selected provider is the last one WHEN transform THEN quote is empty and button is disabled`() {
        // Arrange
        val changellyQuote = quote(provider = changelly)
        val state = buildContentState(
            providers = listOf(changelly),
            quotes = listOf(changellyQuote),
            selectedQuote = changellyQuote,
        )

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        result as SwapAmountUM.Content
        assertThat(result.selectedQuote).isEqualTo(SwapQuoteUM.Empty)
        assertThat(result.swapQuotes).isEmpty()
        assertThat(result.isPrimaryButtonEnabled).isFalse()
    }

    @Test
    fun `GIVEN provider in every currency list WHEN transform THEN it is dropped from all of them`() {
        // Arrange
        val currency = SwapCryptoCurrency(currencyStatus = currencyStatus, providers = listOf(changelly, changeHero))
        val group = SwapCurrenciesGroup(
            available = listOf(currency),
            unavailable = listOf(currency),
            isAfterSearch = false,
            availableForSwap = listOf(currency),
        )
        val state = buildContentState(providers = emptyList(), quotes = emptyList(), selectedQuote = SwapQuoteUM.Empty)
            .copy(swapCurrencies = SwapCurrencies(fromGroup = group, toGroup = group))

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        val expectedCurrency = currency.copy(providers = listOf(changeHero))
        val expectedGroup = group.copy(
            available = listOf(expectedCurrency),
            unavailable = listOf(expectedCurrency),
            availableForSwap = listOf(expectedCurrency),
        )
        result as SwapAmountUM.Content
        assertThat(result.swapCurrencies).isEqualTo(SwapCurrencies(fromGroup = expectedGroup, toGroup = expectedGroup))
    }

    private fun buildTransformer(providerIds: Set<String>) = SwapAmountHideProvidersTransformer(
        providerIds = providerIds,
        secondaryMaximumAmountBoundary = null,
        secondaryMinimumAmountBoundary = null,
        isNeedApplyFCARestrictions = false,
        isBalanceHidden = false,
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

    private fun quote(provider: ExpressProvider) = SwapQuoteUM.Content(
        provider = provider,
        toAmount = BigDecimal.TEN,
        fromAmount = null,
        toAmountValue = TextReference.EMPTY,
        fromAmountValue = TextReference.EMPTY,
        diffPercent = DifferencePercent.Empty,
        isSingleProvider = false,
        rate = TextReference.EMPTY,
        quoteId = null,
        isRestricted = false,
    )

    private fun swapCurrencies(providers: List<ExpressProvider>): SwapCurrencies {
        val group = SwapCurrenciesGroup(
            available = listOf(SwapCryptoCurrency(currencyStatus = currencyStatus, providers = providers)),
            unavailable = emptyList(),
            isAfterSearch = false,
        )
        return SwapCurrencies(fromGroup = group, toGroup = group)
    }

    private fun buildContentState(
        providers: List<ExpressProvider>,
        quotes: List<SwapQuoteUM>,
        selectedQuote: SwapQuoteUM,
    ): SwapAmountUM.Content {
        val primaryStatus = mockk<CryptoCurrencyStatus>(relaxed = true).also { status ->
            every { status.value.amount } returns BigDecimal("1000")
            every { status.value.fiatRate } returns null
            every { status.value.fiatAmount } returns null
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
            swapCurrencies = swapCurrencies(providers = providers),
            swapQuotes = quotes.toPersistentList(),
            selectedQuote = selectedQuote,
            isShowFCAWarning = false,
            appCurrency = null,
            isShowBestRateAnimation = false,
        )
    }
}