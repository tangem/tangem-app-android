package com.tangem.feature.swap.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.states.ProviderState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests for [SwapProviderResolver] — best-provider selection ([SwapProviderResolver.findBest]) and
 * row-badge resolution ([SwapProviderResolver.resolveBadge]).
 *
 * Ranking metric: best provider == lowest `from/to` fiat ratio == highest `to` fiat output for the
 * same `from` input. "Best DEX Rate" prefers the best-rated DEX/DEX_BRIDGE provider when the feature
 * is on and any DEX is present.
 */
internal class SwapProviderResolverTest {

    private val cex1 = provider(id = "cex1", type = ExchangeProviderType.CEX)
    private val cex2 = provider(id = "cex2", type = ExchangeProviderType.CEX)
    private val dex1 = provider(id = "dex1", type = ExchangeProviderType.DEX)
    private val dexBridge = provider(id = "dexBridge", type = ExchangeProviderType.DEX_BRIDGE)

    /** Mirror of the provider ids the resolver treats as FCA restricted. */
    private val fcaRestrictedProviderIds = setOf(
        "changelly",
        "changenow",
        "okx-cross-chain",
        "okx-on-chain",
        "simpleswap",
    )

    // region findBest

    @Test
    fun `GIVEN best dex rate on AND a DEX present WHEN findBest THEN best DEX is selected`() {
        // CEX has the best overall rate (highest output), but a DEX is present.
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best overall
            dex1 to quote(fromFiat = "100", toFiat = "110"), // best among DEX
            dexBridge to quote(fromFiat = "100", toFiat = "105"),
        )

        val best = SwapProviderResolver.findBest(states, isSwapBestDexRateEnabled = true)

        assertThat(best).isEqualTo(dex1)
    }

    @Test
    fun `GIVEN best dex rate on AND no DEX present WHEN findBest THEN best CEX fallback`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "110"),
            cex2 to quote(fromFiat = "100", toFiat = "120"), // best CEX
        )

        val best = SwapProviderResolver.findBest(states, isSwapBestDexRateEnabled = true)

        assertThat(best).isEqualTo(cex2)
    }

    @Test
    fun `GIVEN best dex rate off WHEN findBest THEN best overall regardless of type`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best overall (a CEX)
            dex1 to quote(fromFiat = "100", toFiat = "110"),
        )

        val best = SwapProviderResolver.findBest(states, isSwapBestDexRateEnabled = false)

        assertThat(best).isEqualTo(cex1)
    }

    @Test
    fun `GIVEN DEX_BRIDGE is the best DEX WHEN findBest with best dex rate on THEN DEX_BRIDGE selected`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "130"),
            dex1 to quote(fromFiat = "100", toFiat = "108"),
            dexBridge to quote(fromFiat = "100", toFiat = "115"), // best among DEX-based
        )

        val best = SwapProviderResolver.findBest(states, isSwapBestDexRateEnabled = true)

        assertThat(best).isEqualTo(dexBridge)
    }

    // endregion

    // region resolveBadge

    // --- Both CEX + DEX present, a DEX is the overall best → only "Best rate" (BestTrade) on that DEX.

    @Test
    fun `GIVEN both types present AND DEX is overall best WHEN resolveBadge for that DEX THEN BestTrade`() {
        val states = mapOf(
            dex1 to quote(fromFiat = "100", toFiat = "120"), // best overall AND best DEX
            cex1 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = dex1,
            needApplyFCARestrictions = false,
            state = states.getValue(dex1),
            isSwapBestDexRateEnabled = true,
        )

        // Overall best is the DEX → it gets the single "Best rate" badge, NOT "Best DEX rate".
        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
    }

    @Test
    fun `GIVEN both types present AND DEX is overall best WHEN resolveBadge for the CEX THEN Empty`() {
        val states = mapOf(
            dex1 to quote(fromFiat = "100", toFiat = "120"), // best overall
            cex1 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = false,
            state = states.getValue(cex1),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    // --- Both CEX + DEX present, a CEX is the overall best → BestTrade on the CEX, BestDexRate on the best DEX.

    @Test
    fun `GIVEN both types present AND CEX is overall best WHEN resolveBadge for the CEX THEN BestTrade`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best overall (a CEX)
            dex1 to quote(fromFiat = "100", toFiat = "110"), // best DEX
            dexBridge to quote(fromFiat = "100", toFiat = "105"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = false,
            state = states.getValue(cex1),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
    }

    @Test
    fun `GIVEN both types present AND CEX is overall best WHEN resolveBadge for the best DEX THEN BestDexRate`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best overall (a CEX)
            dex1 to quote(fromFiat = "100", toFiat = "110"), // best DEX
            dexBridge to quote(fromFiat = "100", toFiat = "105"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = dex1,
            needApplyFCARestrictions = false,
            state = states.getValue(dex1),
            isSwapBestDexRateEnabled = true,
        )

        // CEX wins overall, so the best DEX additionally gets the "Best DEX rate" badge.
        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.BestDexRate)
    }

    @Test
    fun `GIVEN both types present AND CEX is overall best WHEN resolveBadge for a non-best DEX THEN Empty`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best overall
            dex1 to quote(fromFiat = "100", toFiat = "110"), // best DEX
            dexBridge to quote(fromFiat = "100", toFiat = "105"), // not the best DEX
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = dexBridge,
            needApplyFCARestrictions = false,
            state = states.getValue(dexBridge),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    // --- DEX-only → only "Best rate" (BestTrade) on the best DEX; no separate "Best DEX rate".

    @Test
    fun `GIVEN DEX-only providers WHEN resolveBadge for the best DEX THEN BestTrade`() {
        val states = mapOf(
            dex1 to quote(fromFiat = "100", toFiat = "120"), // best DEX
            dexBridge to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = dex1,
            needApplyFCARestrictions = false,
            state = states.getValue(dex1),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
    }

    @Test
    fun `GIVEN DEX-only providers WHEN resolveBadge for a non-best DEX THEN Empty`() {
        val states = mapOf(
            dex1 to quote(fromFiat = "100", toFiat = "120"), // best DEX
            dexBridge to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = dexBridge,
            needApplyFCARestrictions = false,
            state = states.getValue(dexBridge),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    // --- CEX-only → only "Best rate" (BestTrade) on the best CEX (no DEX exists).

    @Test
    fun `GIVEN CEX-only providers WHEN resolveBadge for the best CEX THEN BestTrade`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best CEX
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = false,
            state = states.getValue(cex1),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
    }

    // --- Toggle off → only the overall best gets BestTrade; "Best DEX rate" is never produced.

    @Test
    fun `GIVEN toggle off AND both types present with CEX best WHEN resolveBadge for the CEX THEN BestTrade`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best overall
            dex1 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = false,
            state = states.getValue(cex1),
            isSwapBestDexRateEnabled = false,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
    }

    @Test
    fun `GIVEN toggle off AND both types present with CEX best WHEN resolveBadge for the DEX THEN Empty`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best overall
            dex1 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = dex1,
            needApplyFCARestrictions = false,
            state = states.getValue(dex1),
            isSwapBestDexRateEnabled = false,
        )

        // Toggle off → no "Best DEX rate" badge even though a DEX is present and not the overall best.
        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    @Test
    fun `GIVEN FCA restricted provider AND restrictions on WHEN resolveBadge THEN FCAWarningList`() {
        val restricted = provider(id = "changelly", type = ExchangeProviderType.CEX, isRecommended = true)
        val states = mapOf(
            restricted to quote(fromFiat = "100", toFiat = "120"),
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = restricted,
            needApplyFCARestrictions = true,
            state = states.getValue(restricted),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.FCAWarningList)
    }

    @Test
    fun `GIVEN restrictions on WHEN resolveBadge for each FCA restricted id THEN FCAWarningList`() {
        // FCA badge must win regardless of rate — the restricted provider here is NOT the best rate.
        fcaRestrictedProviderIds.forEach { restrictedId ->
            val restricted = provider(id = restrictedId, type = ExchangeProviderType.CEX)
            val states = mapOf(
                restricted to quote(fromFiat = "100", toFiat = "110"),
                cex2 to quote(fromFiat = "100", toFiat = "120"), // best, but not FCA restricted
            )

            val badge = SwapProviderResolver.resolveBadge(
                states = states,
                provider = restricted,
                needApplyFCARestrictions = true,
                state = states.getValue(restricted),
                isSwapBestDexRateEnabled = true,
            )

            assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.FCAWarningList)
        }
    }

    @Test
    fun `GIVEN FCA restricted provider but restrictions off WHEN resolveBadge THEN no FCA badge`() {
        val restricted = provider(id = "changelly", type = ExchangeProviderType.CEX)
        val states = mapOf(
            restricted to quote(fromFiat = "100", toFiat = "120"), // best rated
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = restricted,
            needApplyFCARestrictions = false,
            state = states.getValue(restricted),
            isSwapBestDexRateEnabled = false,
        )

        // Restrictions are off → the restricted id is ignored and the normal best-rate badge wins.
        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.BestTrade)
    }

    @Test
    fun `GIVEN restrictions on for a non-restricted best provider WHEN resolveBadge THEN best rate suppressed`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best & not restricted
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = true,
            state = states.getValue(cex1),
            isSwapBestDexRateEnabled = true,
        )

        // FCA restrictions globally on suppress the best-rate badge even for non-restricted providers.
        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    @Test
    fun `GIVEN permission required WHEN resolveBadge THEN PermissionRequired`() {
        val states = mapOf(
            cex1 to quote(
                fromFiat = "100",
                toFiat = "120",
                permission = PermissionDataState.PermissionRequired(isResetApproval = false, spenderAddress = "0x"),
            ),
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = false,
            state = states.getValue(cex1),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.PermissionRequired)
    }

    @Test
    fun `GIVEN recommended provider WHEN resolveBadge THEN Recommended takes priority over best rate`() {
        val recommended = provider(id = "cex1", type = ExchangeProviderType.CEX, isRecommended = true)
        val states = mapOf(
            recommended to quote(fromFiat = "100", toFiat = "120"), // also the best rate
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = recommended,
            needApplyFCARestrictions = false,
            state = states.getValue(recommended),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Recommended)
    }

    @Test
    fun `GIVEN only one considered provider WHEN resolveBadge THEN Empty`() {
        val states = mapOf(cex1 to quote(fromFiat = "100", toFiat = "120"))

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = false,
            state = states.getValue(cex1),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    @Test
    fun `GIVEN provider is not the best rated WHEN resolveBadge THEN Empty`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"), // best
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex2, // not the best
            needApplyFCARestrictions = false,
            state = states.getValue(cex2),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    @Test
    fun `GIVEN best rated provider but price impact warning WHEN resolveBadge THEN Empty`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120", priceImpactWarning = true), // best, but warning
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = false,
            state = states.getValue(cex1),
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    @Test
    fun `GIVEN no quote state (error row) WHEN resolveBadge THEN Empty`() {
        val states = mapOf(
            cex1 to quote(fromFiat = "100", toFiat = "120"),
            cex2 to quote(fromFiat = "100", toFiat = "110"),
        )

        val badge = SwapProviderResolver.resolveBadge(
            states = states,
            provider = cex1,
            needApplyFCARestrictions = false,
            state = null,
            isSwapBestDexRateEnabled = true,
        )

        assertThat(badge).isEqualTo(ProviderState.AdditionalBadge.Empty)
    }

    // endregion

    private fun provider(
        id: String,
        type: ExchangeProviderType,
        isRecommended: Boolean = false,
    ): SwapProvider = mockk {
        every { providerId } returns id
        every { this@mockk.type } returns type
        every { this@mockk.isRecommended } returns isRecommended
    }

    private fun quote(
        fromFiat: String,
        toFiat: String,
        priceImpactWarning: Boolean = false,
        permission: PermissionDataState = PermissionDataState.Empty,
    ): SwapState.QuotesLoadedState {
        val currency = mockk<CryptoCurrency.Coin> {
            every { decimals } returns 6
        }
        val swapStatus = mockk<SwapCurrencyStatus> {
            every { this@mockk.currency } returns currency
        }
        val fromInfo = mockk<TokenSwapInfo> {
            every { amountFiat } returns BigDecimal(fromFiat)
        }
        val toInfo = mockk<TokenSwapInfo> {
            every { amountFiat } returns BigDecimal(toFiat)
            every { swapCurrencyStatus } returns swapStatus
        }
        return mockk {
            every { fromTokenInfo } returns fromInfo
            every { toTokenInfo } returns toInfo
            every { permissionState } returns permission
            every { priceImpact } returns mockk<PriceImpact> { every { shouldShowWarning() } returns priceImpactWarning }
        }
    }
}