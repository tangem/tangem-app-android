package com.tangem.feature.swap.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
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
    fun `GIVEN best rate AND no FCA AND no permission WHEN buildContentClickable THEN BestTrade badge`() {
        val provider = provider(id = "1inch", isRecommended = false)
        val from = tokenInfo(symbol = "ETH", decimals = 18, amount = BigDecimal.ONE)
        val to = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("3000"))

        val result = SwapProviderStateBuilder.buildContentClickable(
            provider = provider,
            fromTokenInfo = from,
            toTokenInfo = to,
            permissionState = PermissionDataState.Empty,
            selectionType = ProviderState.SelectionType.CLICK,
            isBestRate = true,
            isNeedBestRateBadge = true,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
        assertThat(result.percentLowerThenBest).isEqualTo(PercentDifference.Empty)
        assertThat(result.subtitle).isInstanceOf(TextReference.Str::class.java)
        val subtitle = result.subtitle as TextReference.Str
        assertThat(subtitle.value).contains("ETH")
        assertThat(subtitle.value).contains("USDT")
    }

    @Test
    fun `GIVEN recommended provider WHEN buildContentClickable THEN Recommended badge`() {
        val provider = provider(id = "any", isRecommended = true)
        val info = tokenInfo(symbol = "ETH", decimals = 18, amount = BigDecimal.ONE)

        val result = SwapProviderStateBuilder.buildContentClickable(
            provider = provider,
            fromTokenInfo = info,
            toTokenInfo = info,
            permissionState = PermissionDataState.Empty,
            selectionType = ProviderState.SelectionType.CLICK,
            isBestRate = true,
            isNeedBestRateBadge = true,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.Recommended)
    }

    @Test
    fun `GIVEN permission required WHEN buildContentClickable THEN PermissionRequired badge`() {
        val provider = provider(id = "any", isRecommended = false)
        val info = tokenInfo(symbol = "ETH", decimals = 18, amount = BigDecimal.ONE)

        val result = SwapProviderStateBuilder.buildContentClickable(
            provider = provider,
            fromTokenInfo = info,
            toTokenInfo = info,
            permissionState = PermissionDataState.PermissionRequired(
                isResetApproval = false,
                spenderAddress = "0xspender",
            ),
            selectionType = ProviderState.SelectionType.CLICK,
            isBestRate = true,
            isNeedBestRateBadge = true,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.PermissionRequired)
    }

    @Test
    fun `GIVEN FCA restricted provider WHEN buildContentClickable THEN FCAWarningList badge`() {
        val provider = provider(id = "changelly", isRecommended = true)
        val info = tokenInfo(symbol = "ETH", decimals = 18, amount = BigDecimal.ONE)

        val result = SwapProviderStateBuilder.buildContentClickable(
            provider = provider,
            fromTokenInfo = info,
            toTokenInfo = info,
            permissionState = PermissionDataState.PermissionRequired(
                isResetApproval = false,
                spenderAddress = "0xspender",
            ),
            selectionType = ProviderState.SelectionType.CLICK,
            isBestRate = true,
            isNeedBestRateBadge = true,
            needApplyFCARestrictions = true,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.FCAWarningList)
    }

    @Test
    fun `GIVEN best rate badge disabled WHEN buildContentClickable THEN Empty badge`() {
        val provider = provider(id = "any", isRecommended = false)
        val info = tokenInfo(symbol = "ETH", decimals = 18, amount = BigDecimal.ONE)

        val result = SwapProviderStateBuilder.buildContentClickable(
            provider = provider,
            fromTokenInfo = info,
            toTokenInfo = info,
            permissionState = PermissionDataState.Empty,
            selectionType = ProviderState.SelectionType.CLICK,
            isBestRate = true,
            isNeedBestRateBadge = false,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    @Test
    fun `GIVEN provider WHEN buildContentClickable THEN content carries provider identity`() {
        val provider = provider(id = "1inch", isRecommended = false, name = "1inch", iconUrl = "https://x")
        val info = tokenInfo(symbol = "ETH", decimals = 18, amount = BigDecimal.ONE)

        val result = SwapProviderStateBuilder.buildContentClickable(
            provider = provider,
            fromTokenInfo = info,
            toTokenInfo = info,
            permissionState = PermissionDataState.Empty,
            selectionType = ProviderState.SelectionType.CLICK,
            isBestRate = false,
            isNeedBestRateBadge = false,
            needApplyFCARestrictions = false,
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
        val provider = provider(id = "1inch", isRecommended = false)
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            toTokenInfo = info,
            permissionState = PermissionDataState.Empty,
            pricesLowerBest = mapOf("1inch" to 0.5f),
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.percentLowerThenBest).isEqualTo(PercentDifference.Value(0.5f))
        assertThat(result.subtitle).isInstanceOf(TextReference.Str::class.java)
        val subtitle = result.subtitle as TextReference.Str
        assertThat(subtitle.value).contains("USDT")
    }

    @Test
    fun `GIVEN provider not in pricesLowerBest WHEN buildContentSelectable THEN percentLowerThenBest is zero`() {
        val provider = provider(id = "any", isRecommended = false)
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            toTokenInfo = info,
            permissionState = PermissionDataState.Empty,
            pricesLowerBest = emptyMap(),
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.percentLowerThenBest).isEqualTo(PercentDifference.Value(0f))
    }

    @Test
    fun `GIVEN best rate AND no FCA AND no permission WHEN buildContentSelectable THEN BestTrade badge`() {
        val provider = provider(id = "any", isRecommended = false)
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            toTokenInfo = info,
            permissionState = PermissionDataState.Empty,
            pricesLowerBest = emptyMap(),
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            isBestRate = true,
            isNeedBestRateBadge = true,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
    }

    @Test
    fun `GIVEN isNeedBestRateBadge false WHEN buildContentSelectable THEN no BestTrade badge`() {
        val provider = provider(id = "any", isRecommended = false)
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            toTokenInfo = info,
            permissionState = PermissionDataState.Empty,
            pricesLowerBest = emptyMap(),
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            isBestRate = true,
            isNeedBestRateBadge = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    @Test
    fun `GIVEN isBestRate false AND badge enabled WHEN buildContentSelectable THEN no BestTrade badge`() {
        val provider = provider(id = "any", isRecommended = false)
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            toTokenInfo = info,
            permissionState = PermissionDataState.Empty,
            pricesLowerBest = emptyMap(),
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            isBestRate = false,
            isNeedBestRateBadge = true,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    @Test
    fun `GIVEN permission required WHEN buildContentSelectable THEN PermissionRequired badge`() {
        val provider = provider(id = "any", isRecommended = false)
        val info = tokenInfo(symbol = "USDT", decimals = 6, amount = BigDecimal("100"))

        val result = SwapProviderStateBuilder.buildContentSelectable(
            provider = provider,
            toTokenInfo = info,
            permissionState = PermissionDataState.PermissionRequired(
                isResetApproval = false,
                spenderAddress = "0xspender",
            ),
            pricesLowerBest = emptyMap(),
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.PermissionRequired)
    }

    // endregion

    // region buildAvailableFrom

    @Test
    fun `GIVEN alert text WHEN buildAvailableFrom THEN subtitle is the alert text`() {
        val provider = provider(id = "any", isRecommended = false)
        val alert: TextReference = stringReference("min amount 0.01 ETH")

        val result = SwapProviderStateBuilder.buildAvailableFrom(
            provider = provider,
            alertText = alert,
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.subtitle).isEqualTo(alert)
        assertThat(result.percentLowerThenBest).isEqualTo(PercentDifference.Empty)
    }

    @Test
    fun `GIVEN FCA restricted WHEN buildAvailableFrom THEN FCAWarningList badge`() {
        val provider = provider(id = "okx-on-chain", isRecommended = true)

        val result = SwapProviderStateBuilder.buildAvailableFrom(
            provider = provider,
            alertText = TextReference.EMPTY,
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = true,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.FCAWarningList)
    }

    @Test
    fun `GIVEN recommended WHEN buildAvailableFrom THEN Recommended badge`() {
        val provider = provider(id = "any", isRecommended = true)

        val result = SwapProviderStateBuilder.buildAvailableFrom(
            provider = provider,
            alertText = TextReference.EMPTY,
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.Recommended)
    }

    @Test
    fun `GIVEN no flags WHEN buildAvailableFrom THEN Empty badge`() {
        val provider = provider(id = "any", isRecommended = false)

        val result = SwapProviderStateBuilder.buildAvailableFrom(
            provider = provider,
            alertText = TextReference.EMPTY,
            selectionType = ProviderState.SelectionType.SELECT,
            needApplyFCARestrictions = false,
            onProviderClick = onProviderClick,
        )

        assertThat(result.additionalBadge).isEqualTo(ProviderState.AdditionalBadge.Empty)
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
        isRecommended: Boolean,
        name: String = "Provider",
        iconUrl: String = "https://icon",
    ): SwapProvider = mockk {
        every { providerId } returns id
        every { this@mockk.name } returns name
        every { imageLarge } returns iconUrl
        every { type } returns ExchangeProviderType.DEX
        every { this@mockk.isRecommended } returns isRecommended
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