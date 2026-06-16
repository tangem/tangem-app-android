package com.tangem.feature.swap.converters

import com.tangem.common.ui.swap.SwapRateFormatter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState

/**
 * Builds [ProviderState.Content] for the swap provider list / row.
 */
internal object SwapProviderStateBuilder {

    /**
     * Provider row on the main swap screen — shows the exchange rate `1 base ≈ rate quote`
     * (see [SwapRateFormatter]) and allows the user to open the provider picker.
     */
    fun buildContentClickable(
        provider: SwapProvider,
        state: SwapState.QuotesLoadedState,
        selectionType: ProviderState.SelectionType,
        additionalBadge: ProviderState.AdditionalBadge,
        onProviderClick: (String) -> Unit,
    ): ProviderState.Content {
        val rateString = SwapRateFormatter.formatRate(
            from = state.fromTokenInfo.swapCurrencyStatus.currency,
            to = state.toTokenInfo.swapCurrencyStatus.currency,
            fromAmount = state.fromTokenInfo.tokenAmount.value,
            toAmount = state.toTokenInfo.tokenAmount.value,
        )
        return provider.toContent(
            subtitle = stringReference(rateString),
            additionalBadge = additionalBadge,
            selectionType = selectionType,
            percentLowerThenBest = PercentDifference.Empty,
            approvalSettings = ProviderState.ApprovalSettings.Empty,
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
        state: SwapState.QuotesLoadedState,
        pricesLowerBest: Map<String, Float>,
        selectionType: ProviderState.SelectionType,
        additionalBadge: ProviderState.AdditionalBadge,
        onProviderClick: (String) -> Unit,
        onApprovalSelectClick: (SwapProvider) -> Unit = {},
    ): ProviderState.Content {
        return provider.toContent(
            subtitle = buildSelectableSubtitle(state.toTokenInfo),
            additionalBadge = additionalBadge,
            selectionType = selectionType,
            percentLowerThenBest = pricesLowerBest[provider.providerId]
                ?.let(PercentDifference::Value)
                ?: PercentDifference.Value(0f),
            approvalSettings = when (state.permissionState) {
                is PermissionDataState.PermissionSettings -> ProviderState.ApprovalSettings.Content(
                    onApprovalSelectClick = { onApprovalSelectClick(provider) },
                )
                else -> ProviderState.ApprovalSettings.Empty
            },
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
        additionalBadge: ProviderState.AdditionalBadge,
        onProviderClick: (String) -> Unit,
    ): ProviderState.Content {
        return provider.toContent(
            subtitle = alertText,
            additionalBadge = additionalBadge,
            selectionType = selectionType,
            percentLowerThenBest = PercentDifference.Empty,
            approvalSettings = ProviderState.ApprovalSettings.Empty,
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

    @Suppress("LongParameterList")
    private fun SwapProvider.toContent(
        subtitle: TextReference,
        additionalBadge: ProviderState.AdditionalBadge,
        selectionType: ProviderState.SelectionType,
        percentLowerThenBest: PercentDifference,
        approvalSettings: ProviderState.ApprovalSettings,
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
            approvalSettings = approvalSettings,
        )
    }
}