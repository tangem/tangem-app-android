package com.tangem.features.polymarket.impl.onboarding.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * State of the entry gate shown before the Polymarket feed.
 *
 * The Welcome screen is the gate's only content, so it is not a state of its own: [isStarting] covers both
 * kinds of waiting the gate does — resolving where the user should land, and running the onboarding — because
 * the user cannot tell them apart and the design does not distinguish them either.
 *
 * @property isStarting whether the start button shows its loader. Also true while the entry is still being
 *  resolved, when the button's label is hidden behind that loader anyway.
 * @property startButtonText label of the start button. A user whose account is already part-built is
 *  resuming rather than starting, so the label is state, not a constant. Hidden while [isStarting] is true.
 * @property overlay what covers the content, if anything. Mutually exclusive by construction: the gate can be
 *  region-blocked or failed, never both.
 */
@Immutable
internal data class PolymarketOnboardingUM(
    val isStarting: Boolean,
    val startButtonText: TextReference,
    val onStartClick: () -> Unit,
    val onPolymarketTermsClick: () -> Unit,
    val onTangemTermsClick: () -> Unit,
    val overlay: Overlay? = null,
) {

    /** What covers the Welcome content. */
    @Immutable
    sealed interface Overlay {

        /** The region forbids trading and there is no wallet to fall back to. */
        data class RegionRestrictions(val onDismiss: () -> Unit) : Overlay

        /**
         * The entry decision failed, so the region is unknown and the gate stays shut: dismissing into the
         * feed would let a possibly-restricted user trade. Retry is the only way onward and is always
         * offered — re-resolving is always worth attempting. The user can still leave with back-press.
         *
         * A failed onboarding *run* does not raise this overlay: the Welcome screen is still on screen and
         * its button is already the retry affordance.
         */
        data class Error(val onRetryClick: () -> Unit) : Overlay
    }
}