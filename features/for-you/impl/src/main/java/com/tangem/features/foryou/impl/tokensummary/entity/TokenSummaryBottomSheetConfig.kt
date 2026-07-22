package com.tangem.features.foryou.impl.tokensummary.entity

import kotlinx.serialization.Serializable

/**
 * Navigation config for the single bottom-sheet slot hosted by the token summary component.
 *
 * Both sheets are mutually exclusive — the slot holds at most one child at a time.
 */
@Serializable
internal sealed interface TokenSummaryBottomSheetConfig {

    /** Portfolio selector shown before opening swap in multi-account mode. */
    @Serializable
    data object PortfolioSelector : TokenSummaryBottomSheetConfig

    /** Informational sheet describing the tapped [indicatorType]. */
    @Serializable
    data class Info(val indicatorType: IndicatorType) : TokenSummaryBottomSheetConfig
}