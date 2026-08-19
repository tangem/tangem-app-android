package com.tangem.features.polymarket.impl.onboarding.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * State of the entry gate shown before the Polymarket feed.
 */
@Immutable
internal sealed interface PolymarketOnboardingUM {

    /**
     * The entry decision is not taken yet, so there is nothing to offer the user. Rendered as a bare loader
     * rather than as the Welcome screen: a user who turns out to be onboarded never owed onboarding, and
     * showing them its hero for a frame says otherwise.
     */
    data object Resolving : PolymarketOnboardingUM

    /**
     * The user owes onboarding and is being invited to start it.
     *
     * @property isStarting whether the start button shows its loader in place of its label.
     * @property startButtonText label of the start button. A user whose account is already part-built is
     *  resuming rather than starting, so the label is state, not a constant.
     * @property isRegionRestrictionsShown whether the region-restrictions sheet covers the content.
     */
    data class Welcome(
        val isStarting: Boolean,
        val startButtonText: TextReference,
        val onStartClick: () -> Unit,
        val onPolymarketTermsClick: () -> Unit,
        val onTangemTermsClick: () -> Unit,
        val isRegionRestrictionsShown: Boolean = false,
        val onRegionRestrictionsDismiss: () -> Unit = {},
    ) : PolymarketOnboardingUM
}