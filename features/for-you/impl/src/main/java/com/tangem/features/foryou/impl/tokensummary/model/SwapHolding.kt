package com.tangem.features.foryou.impl.tokensummary.model

import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason

/**
 * A single holding of the summary token together with what a swap from it would run into.
 *
 * A holding is never dropped for being unswappable: the user still sees it and, when they pick it, is told the
 * [unavailabilityReason] instead of being taken to Swap.
 */
internal data class SwapHolding(
    val entry: TokenSelectorEntry,
    val unavailabilityReason: ScenarioUnavailabilityReason,
) {

    val isSwapAvailable: Boolean
        get() = unavailabilityReason == ScenarioUnavailabilityReason.None
}