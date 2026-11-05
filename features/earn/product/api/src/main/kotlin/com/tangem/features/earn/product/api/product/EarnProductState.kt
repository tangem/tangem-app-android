package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.product.api.session.EarnAction

sealed interface EarnProductState {

    data object Loading : EarnProductState

    /** No data at all: the chip is disabled, the screen offers Retry. */
    data object Unavailable : EarnProductState

    data class Ready(
        val rate: EarnProductRate,
        val description: TextReference,
        val projection: EarnProjection?,
        val benefits: List<EarnTextBlock>,
        val howItWorks: EarnHowItWorks?,
        val details: List<EarnRow>,
        val limits: EarnLimits,
        val apyPeriods: List<EarnPeriod>,
        val primary: EarnActionItem,
        val secondary: List<EarnActionItem> = emptyList(),
        val freshness: EarnFreshness?,
        val position: EarnPositionState,
    ) : EarnProductState
}

/**
 * A button of the state: the action, why it cannot be done now, and the button caption.
 *
 * @property block `null`: the action is available; otherwise the button is dimmed and a click shows `block.reason`
 * @property label the caption of the button, equal to the action label by default; the action itself goes to the route
 */
data class EarnActionItem(
    val action: EarnAction,
    val block: EarnBlock? = null,
    val label: TextReference = action.label,
)