package com.tangem.features.foryou.impl.tokensummary.entity

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Navigation config for the single bottom-sheet slot hosted by the token summary component.
 *
 * Sheets are mutually exclusive — the slot holds at most one child at a time — and are not saved across process
 * death, so a config only has to carry what its sheet needs while it is open.
 */
internal sealed interface TokenSummaryBottomSheetConfig {

    /**
     * Token chooser shown before opening Swap when the summary token is held, and swappable, in several accounts.
     * Carries no data: the holdings it renders are the ones the model already resolved.
     */
    data object SwapChooser : TokenSummaryBottomSheetConfig

    /** Top-up options for the summary token, shown when none of its holdings has a balance. */
    data class ManageFunds(val rawCurrencyId: CryptoCurrency.RawID) : TokenSummaryBottomSheetConfig

    /** Informational sheet describing the tapped [indicatorType]. */
    data class Info(val indicatorType: IndicatorType) : TokenSummaryBottomSheetConfig
}