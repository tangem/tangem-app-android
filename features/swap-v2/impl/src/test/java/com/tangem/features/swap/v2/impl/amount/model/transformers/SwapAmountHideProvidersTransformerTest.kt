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
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    fun `GIVEN providers already hidden WHEN transform THEN same state instance is returned`() {
        // Arrange
        val changeHeroQuote = quote(provider = changeHero)
        val state = buildContentState(
            providers = listOf(changeHero),
            quotes = listOf(changeHeroQuote),
            selectedQuote = changeHeroQuote,
        )

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        assertThat(result).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN selected provider is hidden WHEN transform THEN it is dropped and quote is loading`() {
        // Arrange
        val changellyQuote = quote(provider = changelly)
        val changeHeroQuote = quote(provider = changeHero)
        val state = buildContentState(
            providers = listOf(changelly, changeHero),
            quotes = listOf(changellyQuote, changeHeroQuote),
            selectedQuote = changellyQuote,
        )

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        val expected = state.copy(
            swapCurrencies = swapCurrencies(providers = listOf(changeHero)),
            swapQuotes = persistentListOf(changeHeroQuote),
            selectedQuote = SwapQuoteUM.Loading,
            isPrimaryButtonEnabled = false,
        )
        assertThat(result).isEqualTo(expected)
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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN remaining providers WHEN transform THEN input is aligned to their rate mode`(model: AlignModel) {
        // Arrange
        val fixedAndFloat = changelly.copy(rateTypes = listOf(ExpressRateType.Fixed, ExpressRateType.Float))
        val remaining = changeHero.copy(rateTypes = model.remainingRateTypes)
        val state = buildContentState(
            providers = listOf(fixedAndFloat, remaining),
            quotes = emptyList(),
            selectedQuote = SwapQuoteUM.Empty,
        ).copy(
            secondaryCryptoCurrencyStatus = currencyStatus,
            swapRateMode = SwapRateMode.FLOAT_AND_FIXED,
            selectedAmountType = model.amountType,
            swapRateType = model.rateType,
        )

        // Act
        val result = buildTransformer(providerIds = setOf(changelly.providerId)).transform(state)

        // Assert
        val expected = state.copy(
            swapCurrencies = swapCurrencies(providers = listOf(remaining)),
            selectedQuote = SwapQuoteUM.Loading,
            isPrimaryButtonEnabled = false,
            swapRateMode = model.expectedMode,
            selectedAmountType = model.expectedAmountType,
            swapRateType = model.expectedRateType,
        )
        assertThat(result).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        AlignModel(
            remainingRateTypes = listOf(ExpressRateType.Float),
            amountType = SwapAmountType.To,
            rateType = ExpressRateType.Fixed,
            expectedMode = SwapRateMode.FLOAT_ONLY,
            expectedAmountType = SwapAmountType.From,
            expectedRateType = ExpressRateType.Float,
        ),
        AlignModel(
            remainingRateTypes = listOf(ExpressRateType.Float),
            amountType = SwapAmountType.From,
            rateType = ExpressRateType.Float,
            expectedMode = SwapRateMode.FLOAT_ONLY,
            expectedAmountType = SwapAmountType.From,
            expectedRateType = ExpressRateType.Float,
        ),
        AlignModel(
            remainingRateTypes = listOf(ExpressRateType.Fixed),
            amountType = SwapAmountType.From,
            rateType = ExpressRateType.Float,
            expectedMode = SwapRateMode.FIXED_ONLY,
            expectedAmountType = SwapAmountType.From,
            expectedRateType = ExpressRateType.Fixed,
        ),
        AlignModel(
            remainingRateTypes = listOf(ExpressRateType.Fixed),
            amountType = SwapAmountType.To,
            rateType = ExpressRateType.Fixed,
            expectedMode = SwapRateMode.FIXED_ONLY,
            expectedAmountType = SwapAmountType.To,
            expectedRateType = ExpressRateType.Fixed,
        ),
        AlignModel(
            remainingRateTypes = listOf(ExpressRateType.Fixed, ExpressRateType.Float),
            amountType = SwapAmountType.From,
            rateType = ExpressRateType.Float,
            expectedMode = SwapRateMode.FLOAT_AND_FIXED,
            expectedAmountType = SwapAmountType.From,
            expectedRateType = ExpressRateType.Float,
        ),
    )

    data class AlignModel(
        val remainingRateTypes: List<ExpressRateType>,
        val amountType: SwapAmountType,
        val rateType: ExpressRateType,
        val expectedMode: SwapRateMode,
        val expectedAmountType: SwapAmountType,
        val expectedRateType: ExpressRateType,
    )

    private fun buildTransformer(providerIds: Set<String>) = SwapAmountHideProvidersTransformer(
        providerIds = providerIds,
        isBalanceHidden = false,
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