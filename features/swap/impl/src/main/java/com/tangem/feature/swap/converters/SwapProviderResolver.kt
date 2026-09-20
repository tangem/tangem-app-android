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
 *  - [findBest] — which provider is the "best" among the loaded quotes,
 *  - [preferAvailable] — which quotes are eligible for those decisions at all, and
 *  - [resolveBadge] / [resolveListBadge] — which [ProviderState.AdditionalBadge] a provider row should show.
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
     * Region-restricted quotes are left out of the ranking (see [preferAvailable]). When at least one
     * DEX/DEX_BRIDGE provider is present, the best-rated DEX provider wins; otherwise the overall
     * best-rated provider is returned (the best CEX when no DEX is available). "Best rated" = lowest
     * from/to fiat ratio (most output per unit of input). Returns null when [states] is empty.
     */
    fun findBest(states: Map<SwapProvider, SwapState.QuotesLoadedState>): SwapProvider? {
        val availableStates = preferAvailable(states)
        val dexStates = availableStates.filterKeys { it.type.isDex() }
        return if (dexStates.isNotEmpty()) {
            findBestRated(dexStates)
        } else {
            findBestRated(availableStates)
        }
    }

    /**
     * Quotes the user can actually swap through: a region-restricted quote is displayable but not
     * executable, so it wins neither the best-rate ranking nor the automatic provider selection. When
     * every quote is restricted the full set stands in, so a provider is still picked instead of
     * leaving the form without one.
     */
    fun <T : SwapState> preferAvailable(states: Map<SwapProvider, T>): Map<SwapProvider, T> {
        return states
            .filterValues { state -> (state as? SwapState.QuotesLoadedState)?.isRestricted != true }
            .ifEmpty { states }
    }

    /**
     * Resolves the badge for the provider block on the swap form.
     *
     * Priority: FCA restriction → permission required → recommended → best rate → none. A best-rate
     * badge is shown only when more than one provider is considered, FCA restrictions are not applied,
     * and this row's quote carries no price-impact warning. Which best-rate badge it is depends on the
     * provider mix:
     *  - [AdditionalBadge.BestTrade] ("Best rate") — always on the overall best-rated provider,
     *    regardless of its type.
     *  - [AdditionalBadge.BestDexRate] ("Best DEX rate") — only when both CEX and DEX providers are
     *    present and a CEX is the overall best (so the best DEX is not the overall best); it is then
     *    shown on the best-rated DEX. When a DEX already is the overall best, or the set is CEX-only /
     *    DEX-only, no separate "Best DEX rate" badge is shown.
     *
     * @param states all loaded quotes — used to find the best providers and to count considered providers.
     * @param provider the provider this row represents.
     * @param needApplyFCARestrictions whether FCA restrictions apply to the current user.
     * @param state this provider's [SwapState]; price-impact and permission are read from it when it
     * is a [SwapState.QuotesLoadedState]. Null for error rows (which only resolve to FCA / recommended / none).
     */
    fun resolveBadge(
        states: Map<SwapProvider, SwapState.QuotesLoadedState>,
        provider: SwapProvider,
        needApplyFCARestrictions: Boolean,
        state: SwapState? = null,
    ): AdditionalBadge {
        val priceImpact = (state as? SwapState.QuotesLoadedState)?.priceImpact
        val permissionState = (state as? SwapState.QuotesLoadedState)?.permissionState

        val isNeedBestRateBadge = states.consideredProvidersStates().size > 1
        val isBestRateBadgeAllowed = !needApplyFCARestrictions && isNeedBestRateBadge &&
            priceImpact != null && !priceImpact.shouldShowWarning()

        return when {
            needApplyFCARestrictions && isFCARestricted(provider) -> AdditionalBadge.FCAWarningList
            permissionState is PermissionDataState.PermissionRequired -> AdditionalBadge.PermissionRequired
            provider.isRecommended -> AdditionalBadge.Recommended
            isBestRateBadgeAllowed -> resolveBestRateBadge(states, provider)
            else -> AdditionalBadge.Empty
        }
    }

    /**
     * Resolves the badge for a provider row in the picker, where the user chooses between providers.
     *
     * Same as [resolveBadge], except a region-restricted quote reports that it cannot be traded instead
     * of carrying a badge that invites the user into it. The form block keeps [resolveBadge]: it
     * describes the provider already selected, not a choice between them.
     */
    fun resolveListBadge(
        states: Map<SwapProvider, SwapState.QuotesLoadedState>,
        provider: SwapProvider,
        needApplyFCARestrictions: Boolean,
        state: SwapState? = null,
    ): AdditionalBadge {
        if ((state as? SwapState.QuotesLoadedState)?.isRestricted == true) {
            return AdditionalBadge.Unavailable
        }

        return resolveBadge(
            states = states,
            provider = provider,
            needApplyFCARestrictions = needApplyFCARestrictions,
            state = state,
        )
    }

    /** Whether [provider] is on the FCA warning list, regardless of which badge its row ends up with. */
    fun isFCARestricted(provider: SwapProvider): Boolean = provider.providerId in FCA_RESTRICTED_PROVIDER_IDS

    /**
     * Picks the best-rate badge for [provider] once it has passed the eligibility gate in [resolveBadge].
     * Returns [AdditionalBadge.Empty] when this row is neither the overall best nor the eligible best DEX.
     */
    private fun resolveBestRateBadge(
        states: Map<SwapProvider, SwapState.QuotesLoadedState>,
        provider: SwapProvider,
    ): AdditionalBadge {
        val availableStates = preferAvailable(states)
        val overallBest = findBestRated(availableStates)
        val isOverallBest = provider.providerId == overallBest?.providerId

        val dexStates = availableStates.filterKeys { it.type.isDex() }
        val hasDex = dexStates.isNotEmpty()
        val hasCex = availableStates.keys.any { !it.type.isDex() }
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