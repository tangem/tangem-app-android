package com.tangem.feature.swap.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale

/**
 * Tests for [SwapProviderStateBuilder].
 *
 * Badge selection lives in [SwapProviderResolver]; here the badge is supplied as [additionalBadge]
 * and the builder is expected to render it verbatim. These tests therefore focus on the builder's
 * own responsibilities — subtitle formatting, percent-delta mapping, provider identity, and passing
 * the badge through — not on badge-decision logic.
 */
internal class SwapProviderStateBuilderTest {

    private var originalLocale: Locale = Locale.getDefault()

    private val onProviderClick: (String) -> Unit = {}

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    // region buildContentClickable

    @Test
    fun `GIVEN a badge WHEN buildContentClickable THEN it is rendered with a rate subtitle`() {
        val provider = provider(id = "1inch")
        val from = tokenInfo(symbol = "ETH", decimals = 18, amount = BigDecimal.ONE)
        val to = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("3000"))

        val result = SwapProviderStateBuilder.buildContentClickable(
            provider = provider,
            state = quoteState(toTokenInfo = to, fromTokenInfo = from),
            selectionType = ProviderState.SelectionType.CLICK,
            additionalBadge = ProviderState.AdditionalBadge.BestDexRate,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.BestDexRate)
        assertThat(result.percentLowerThenBest).isEqualTo(PercentDifference.Empty)
        assertThat(result.subtitle).isInstanceOf(TextReference.Str::class.java)
        val subtitle = result.subtitle as TextReference.Str
        assertThat(subtitle.value).contains("ETH")
        assertThat(subtitle.value).contains("USDT")
    }

    @Test
    fun `GIVEN provider WHEN buildContentClickable THEN content carries provider identity`() {
        val provider = provider(id = "1inch", name = "1inch", iconUrl = "https://x")
        val info = tokenInfo(symbol = "ETH", decimals = 18, amount = BigDecimal.ONE)

        val result = SwapProviderStateBuilder.buildContentClickable(
            provider = provider,
            state = quoteState(toTokenInfo = info),
            selectionType = ProviderState.SelectionType.CLICK,
            additionalBadge = ProviderState.AdditionalBadge.Empty,
            onProviderClick = onProviderClick,
        )

        assertThat(result.id).isEqualTo("1inch")
        assertThat(result.name).isEqualTo("1inch")
        assertThat(result.iconUrl).isEqualTo("https://x")
        assertThat(result.type).isEqualTo("DEX")
        assertThat(result.selectionType).isEqualTo(ProviderState.SelectionType.CLICK)
        assertThat(result.namePrefix).isEqualTo(ProviderState.PrefixType.NONE)
    }

    // endregion

    // region buildContentSelectable

    @Test
    fun `GIVEN provider in pricesLowerBest WHEN buildContentSelectable THEN percentLowerThenBest is mapped`() {
        val provider = provider(id = "1inch")
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            state = quoteState(toTokenInfo = info),
            pricesLowerBest = mapOf("1inch" to 0.5f),
            selectionType = ProviderState.SelectionType.SELECT,
            additionalBadge = ProviderState.AdditionalBadge.Empty,
            onProviderClick = onProviderClick,
        )

        assertThat(result.percentLowerThenBest).isEqualTo(PercentDifference.Value(0.5f))
        assertThat(result.subtitle).isInstanceOf(TextReference.Str::class.java)
        val subtitle = result.subtitle as TextReference.Str
        assertThat(subtitle.value).contains("USDT")
    }

    @Test
    fun `GIVEN provider not in pricesLowerBest WHEN buildContentSelectable THEN percentLowerThenBest is zero`() {
        val provider = provider(id = "any")
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            state = quoteState(toTokenInfo = info),
            pricesLowerBest = emptyMap(),
            selectionType = ProviderState.SelectionType.SELECT,
            additionalBadge = ProviderState.AdditionalBadge.Empty,
            onProviderClick = onProviderClick,
        )

        assertThat(result.percentLowerThenBest).isEqualTo(PercentDifference.Value(0f))
    }

    @Test
    fun `GIVEN a badge WHEN buildContentSelectable THEN it is rendered`() {
        val provider = provider(id = "any")
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            state = quoteState(toTokenInfo = info),
            pricesLowerBest = emptyMap(),
            selectionType = ProviderState.SelectionType.SELECT,
            additionalBadge = ProviderState.AdditionalBadge.BestTrade,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
        assertThat(result.subtitle).isInstanceOf(TextReference.Str::class.java)
        assertThat((result.subtitle as TextReference.Str).value).contains("USDT")
    }

    // endregion

    // region buildAvailableFrom

    @Test
    fun `GIVEN alert text WHEN buildAvailableFrom THEN subtitle is the alert text and badge is rendered`() {
        val provider = provider(id = "any")
        val alert: TextReference = stringReference("min amount 0.01 ETH")

        val result = SwapProviderStateBuilder.buildAvailableFrom(
            provider = provider,
            alertText = alert,
            selectionType = ProviderState.SelectionType.SELECT,
            additionalBadge = ProviderState.AdditionalBadge.FCAWarningList,
            onProviderClick = onProviderClick,
        )

        assertThat(result.subtitle).isEqualTo(alert)
        assertThat(result.percentLowerThenBest).isEqualTo(PercentDifference.Empty)
        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.FCAWarningList)
    }

    // endregion

    // region buildSelectableSubtitle

    @Test
    fun `GIVEN to token info WHEN buildSelectableSubtitle THEN string contains symbol`() {
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildSelectableSubtitle(info)

        assertThat(result).isInstanceOf(TextReference.Str::class.java)
        val subtitle = result as TextReference.Str
        assertThat(subtitle.value).contains("USDT")
        assertThat(subtitle.value).contains("100")
    }

    // endregion

    private fun provider(
        id: String,
        name: String = "Provider",
        iconUrl: String = "https://icon",
    ): SwapProvider = mockk {
        every { providerId } returns id
        every { this@mockk.name } returns name
        every { imageLarge } returns iconUrl
        every { type } returns ExchangeProviderType.DEX
    }

    private fun quoteState(
        toTokenInfo: TokenSwapInfo,
        fromTokenInfo: TokenSwapInfo = toTokenInfo,
        permissionState: PermissionDataState = PermissionDataState.Empty,
    ): SwapState.QuotesLoadedState = mockk {
        every { this@mockk.fromTokenInfo } returns fromTokenInfo
        every { this@mockk.toTokenInfo } returns toTokenInfo
        every { this@mockk.permissionState } returns permissionState
    }

    private fun tokenInfo(symbol: String, decimals: Int, amount: BigDecimal): TokenSwapInfo {
        val currency = mockk<CryptoCurrency.Coin> {
            every { this@mockk.symbol } returns symbol
            every { this@mockk.decimals } returns decimals
        }
        val swapStatus = mockk<SwapCurrencyStatus> {
            every { this@mockk.currency } returns currency
        }
        return TokenSwapInfo(
            tokenAmount = SwapAmount(value = amount, decimals = decimals),
            amountFiat = BigDecimal.ZERO,
            swapCurrencyStatus = swapStatus,
        )
    }
}