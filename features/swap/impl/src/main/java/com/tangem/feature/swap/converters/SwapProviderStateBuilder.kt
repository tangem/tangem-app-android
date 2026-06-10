package com.tangem.feature.swap.converters

import com.tangem.common.ui.swap.SwapRateFormatter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState

/**
 * Builds [ProviderState.Content] for the swap provider list / row.
 *
 * Pure: takes everything it needs as parameters. Designed to be unit-tested in isolation.
 */
internal object SwapProviderStateBuilder {

    private val FCA_RESTRICTED_PROVIDER_IDS = setOf(
        "changelly",
        "changenow",
        "okx-cross-chain",
        "okx-on-chain",
        "simpleswap",
    )

    /**
     * Provider row on the main swap screen — shows the exchange rate `1 base ≈ rate quote`
     * (see [SwapRateFormatter]) and allows the user to open the provider picker.
     */
    @Suppress("LongParameterList")
    fun buildContentClickable(
        provider: SwapProvider,
        fromTokenInfo: TokenSwapInfo,
        toTokenInfo: TokenSwapInfo,
        permissionState: PermissionDataState,
        selectionType: ProviderState.SelectionType,
        isBestRate: Boolean,
        isNeedBestRateBadge: Boolean,
        needApplyFCARestrictions: Boolean,
        onProviderClick: (String) -> Unit,
    ): ProviderState.Content {
        val rateString = SwapRateFormatter.formatRate(
            from = fromTokenInfo.swapCurrencyStatus.currency,
            to = toTokenInfo.swapCurrencyStatus.currency,
            fromAmount = fromTokenInfo.tokenAmount.value,
            toAmount = toTokenInfo.tokenAmount.value,
        )
        return provider.toContent(
            subtitle = stringReference(rateString),
            additionalBadge = resolveBadge(
                provider = provider,
                needApplyFCARestrictions = needApplyFCARestrictions,
                permissionState = permissionState,
                isBestRate = isBestRate,
                isNeedBestRateBadge = isNeedBestRateBadge,
            ),
            selectionType = selectionType,
            percentLowerThenBest = PercentDifference.Empty,
            onProviderClick = onProviderClick,
        )
    }

    /**
     * Provider row in the provider-picker bottom sheet. Subtitle shows the formatted *to* amount
     * (not a rate) and the row carries a percentage delta vs. the best rate.
     */
    @Suppress("LongParameterList")
    fun buildContentSelectable(
        provider: SwapProvider,
        toTokenInfo: TokenSwapInfo,
        permissionState: PermissionDataState,
        pricesLowerBest: Map<String, Float>,
        selectionType: ProviderState.SelectionType,
        isBestRate: Boolean = false,
        isNeedBestRateBadge: Boolean = false,
        needApplyFCARestrictions: Boolean,
        onProviderClick: (String) -> Unit,
    ): ProviderState.Content {
        return provider.toContent(
            subtitle = buildSelectableSubtitle(toTokenInfo),
            additionalBadge = resolveBadge(
                provider = provider,
                needApplyFCARestrictions = needApplyFCARestrictions,
                permissionState = permissionState,
                isBestRate = isBestRate,
                isNeedBestRateBadge = isNeedBestRateBadge,
            ),
            selectionType = selectionType,
            percentLowerThenBest = pricesLowerBest[provider.providerId]
                ?.let(PercentDifference::Value)
                ?: PercentDifference.Value(0f),
            onProviderClick = onProviderClick,
        )
    }

    /**
     * Provider row for an unavailable / errored provider — subtitle is the error/alert text
     * resolved by the caller.
     */
    fun buildAvailableFrom(
        provider: SwapProvider,
        alertText: TextReference,
        selectionType: ProviderState.SelectionType,
        needApplyFCARestrictions: Boolean,
        onProviderClick: (String) -> Unit,
    ): ProviderState.Content {
        return provider.toContent(
            subtitle = alertText,
            additionalBadge = resolveBadge(
                provider = provider,
                needApplyFCARestrictions = needApplyFCARestrictions,
            ),
            selectionType = selectionType,
            percentLowerThenBest = PercentDifference.Empty,
            onProviderClick = onProviderClick,
        )
    }

    /**
     * Subtitle (formatted *to* amount) used both for picker rows and when refreshing
     * the provider-picker bottom sheet. Single source of truth so both paths stay in sync.
     */
    fun buildSelectableSubtitle(toTokenInfo: TokenSwapInfo): TextReference {
        val toAmount = toTokenInfo.tokenAmount.value.format {
            crypto(toTokenInfo.swapCurrencyStatus.currency)
        }
        return stringReference(toAmount)
    }

    private fun resolveBadge(
        provider: SwapProvider,
        needApplyFCARestrictions: Boolean,
        permissionState: PermissionDataState? = null,
        isBestRate: Boolean = false,
        isNeedBestRateBadge: Boolean = false,
    ): ProviderState.AdditionalBadge {
        return when {
            needApplyFCARestrictions && provider.isFCARestricted() ->
                ProviderState.AdditionalBadge.FCAWarningList
            permissionState is PermissionDataState.PermissionRequired ->
                ProviderState.AdditionalBadge.PermissionRequired
            provider.isRecommended ->
                ProviderState.AdditionalBadge.Recommended
            isNeedBestRateBadge && isBestRate && !needApplyFCARestrictions ->
                ProviderState.AdditionalBadge.BestTrade
            else ->
                ProviderState.AdditionalBadge.Empty
        }
    }

    private fun SwapProvider.toContent(
        subtitle: TextReference,
        additionalBadge: ProviderState.AdditionalBadge,
        selectionType: ProviderState.SelectionType,
        percentLowerThenBest: PercentDifference,
        onProviderClick: (String) -> Unit,
    ): ProviderState.Content {
        return ProviderState.Content(
            id = providerId,
            name = name,
            iconUrl = imageLarge,
            type = type.providerName,
            subtitle = subtitle,
            additionalBadge = additionalBadge,
            selectionType = selectionType,
            percentLowerThenBest = percentLowerThenBest,
            namePrefix = ProviderState.PrefixType.NONE,
            onProviderClick = onProviderClick,
        )
    }

    private fun SwapProvider.isFCARestricted(): Boolean = providerId in FCA_RESTRICTED_PROVIDER_IDS
}