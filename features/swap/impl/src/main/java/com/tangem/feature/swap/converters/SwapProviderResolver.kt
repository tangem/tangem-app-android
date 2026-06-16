package com.tangem.feature.swap.converters

import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.model.consideredProvidersStates
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.models.states.ProviderState.AdditionalBadge
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure provider-level decisions for the swap UI:
 *  - [findBest] — which provider is the "best" among the loaded quotes, and
 *  - [resolveBadge] — which [ProviderState.AdditionalBadge] a provider row should show.
 */
internal object SwapProviderResolver {

    private val FCA_RESTRICTED_PROVIDER_IDS = setOf(
        "changelly",
        "changenow",
        "okx-cross-chain",
        "okx-on-chain",
        "simpleswap",
    )

    /**
     * Picks the best provider among [states].
     *
     * When [isSwapBestDexRateEnabled] is on and at least one DEX/DEX_BRIDGE provider is present, the
     * best-rated DEX provider wins; otherwise the overall best-rated provider is returned (the best
     * CEX when no DEX is available). "Best rated" = lowest from/to fiat ratio (most output per unit
     * of input). Returns null when [states] is empty.
     *
     * @param isSwapBestDexRateEnabled whether the Best DEX Rate feature toggle is on.
     */
    fun findBest(
        states: Map<SwapProvider, SwapState.QuotesLoadedState>,
        isSwapBestDexRateEnabled: Boolean,
    ): SwapProvider? {
        if (!isSwapBestDexRateEnabled) return findBestRated(states)
        val dexStates = states.filterKeys { it.type.isDex() }
        return if (dexStates.isNotEmpty()) {
            findBestRated(dexStates)
        } else {
            findBestRated(states)
        }
    }

    /**
     * Resolves the badge for a single provider row.
     *
     * Priority: FCA restriction → permission required → recommended → best rate → none. A best-rate
     * badge is shown only when more than one provider is considered, FCA restrictions are not applied,
     * and this row's quote carries no price-impact warning. Which best-rate badge it is depends on the
     * provider mix (only relevant when [isSwapBestDexRateEnabled] is on):
     *  - [AdditionalBadge.BestTrade] ("Best rate") — always on the overall best-rated provider,
     *    regardless of its type.
     *  - [AdditionalBadge.BestDexRate] ("Best DEX rate") — only when both CEX and DEX providers are
     *    present and a CEX is the overall best (so the best DEX is not the overall best); it is then
     *    shown on the best-rated DEX. When a DEX already is the overall best, or the set is CEX-only /
     *    DEX-only, no separate "Best DEX rate" badge is shown.
     *
     * When [isSwapBestDexRateEnabled] is off, only the overall best provider gets [AdditionalBadge.BestTrade]
     * (legacy behaviour) and [AdditionalBadge.BestDexRate] is never produced.
     *
     * @param states all loaded quotes — used to find the best providers and to count considered providers.
     * @param provider the provider this row represents.
     * @param needApplyFCARestrictions whether FCA restrictions apply to the current user.
     * @param state this provider's [SwapState]; price-impact and permission are read from it when it
     * is a [SwapState.QuotesLoadedState]. Null for error rows (which only resolve to FCA / recommended / none).
     * @param isSwapBestDexRateEnabled whether the Best DEX Rate feature toggle is on.
     */
    fun resolveBadge(
        states: Map<SwapProvider, SwapState.QuotesLoadedState>,
        provider: SwapProvider,
        needApplyFCARestrictions: Boolean,
        state: SwapState? = null,
        isSwapBestDexRateEnabled: Boolean,
    ): AdditionalBadge {
        val priceImpact = (state as? SwapState.QuotesLoadedState)?.priceImpact
        val permissionState = (state as? SwapState.QuotesLoadedState)?.permissionState

        val isNeedBestRateBadge = states.consideredProvidersStates().size > 1
        val isBestRateBadgeAllowed = !needApplyFCARestrictions && isNeedBestRateBadge &&
            priceImpact != null && !priceImpact.shouldShowWarning()

        return when {
            needApplyFCARestrictions && provider.isFCARestricted() -> AdditionalBadge.FCAWarningList
            permissionState is PermissionDataState.PermissionRequired -> AdditionalBadge.PermissionRequired
            provider.isRecommended -> AdditionalBadge.Recommended
            isBestRateBadgeAllowed -> resolveBestRateBadge(states, provider, isSwapBestDexRateEnabled)
            else -> AdditionalBadge.Empty
        }
    }

    /**
     * Picks the best-rate badge for [provider] once it has passed the eligibility gate in [resolveBadge].
     * Returns [AdditionalBadge.Empty] when this row is neither the overall best nor the eligible best DEX.
     */
    private fun resolveBestRateBadge(
        states: Map<SwapProvider, SwapState.QuotesLoadedState>,
        provider: SwapProvider,
        isSwapBestDexRateEnabled: Boolean,
    ): AdditionalBadge {
        val overallBest = findBestRated(states)
        val isOverallBest = provider.providerId == overallBest?.providerId

        // Toggle off → legacy behaviour: only the overall best provider gets the "Best rate" badge.
        if (!isSwapBestDexRateEnabled) {
            return if (isOverallBest) AdditionalBadge.BestTrade else AdditionalBadge.Empty
        }

        val dexStates = states.filterKeys { it.type.isDex() }
        val hasDex = dexStates.isNotEmpty()
        val hasCex = states.keys.any { !it.type.isDex() }
        val bestDex = findBestRated(dexStates)
        val isBestDex = bestDex != null && provider.providerId == bestDex.providerId
        // Both types present and a CEX is the overall best (i.e. overall best != best DEX).
        val isCexBeatsDex = hasDex && hasCex && overallBest?.providerId != bestDex?.providerId

        return when {
            isOverallBest -> AdditionalBadge.BestTrade
            isCexBeatsDex && isBestDex -> AdditionalBadge.BestDexRate
            else -> AdditionalBadge.Empty
        }
    }

    /** Best provider following the default best-rate behaviour over all providers. */
    private fun findBestRated(states: Map<SwapProvider, SwapState.QuotesLoadedState>): SwapProvider? {
        return states.minByOrNull { entry -> entry.value.rateRatio() }?.key
    }

    private fun SwapProvider.isFCARestricted(): Boolean = providerId in FCA_RESTRICTED_PROVIDER_IDS

    private fun SwapState.QuotesLoadedState.rateRatio(): BigDecimal {
        val fromAmountFiat = fromTokenInfo.amountFiat
        val toAmountFiat = toTokenInfo.amountFiat
        return if (!fromAmountFiat.isNullOrZero() && !toAmountFiat.isNullOrZero()) {
            fromAmountFiat.divide(
                toAmountFiat,
                toTokenInfo.swapCurrencyStatus.currency.decimals,
                RoundingMode.HALF_UP,
            )
        } else {
            BigDecimal.ZERO
        }
    }
}