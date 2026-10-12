package com.tangem.features.polymarket.impl.onboarding.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * State of the entry gate shown before the Polymarket feed.
 */
@Immutable
internal sealed interface PolymarketOnboardingUM {

    /**
     * Rendered as a bare loader, not as the Welcome screen: a user who turns out to be onboarded never owed
     * onboarding, and showing them its hero for a frame says otherwise.
     */
    data object Resolving : PolymarketOnboardingUM

    /**
     * A state that offers the card tap and shares the footer with every other such state. The progress updates
     * of a run apply to all of them, which is why they answer to one type rather than to [Welcome] alone.
     *
     * @property actionButtonText label of the action button. A user whose account is already part-built is
     *  resuming rather than starting, so the label is state, not a constant.
     */
    sealed interface Action : PolymarketOnboardingUM {

        val isInProgress: Boolean
        val actionButtonText: TextReference
        val onActionClick: () -> Unit
        val onPolymarketTermsClick: () -> Unit
        val onTangemTermsClick: () -> Unit

        /** @return this state with [isInProgress] replaced, so a run can be reflected without knowing which state it is */
        fun copyInProgress(isInProgress: Boolean): Action
    }

    /** The user owes onboarding and is being invited to start it. */
    data class Welcome(
        override val isInProgress: Boolean,
        override val actionButtonText: TextReference,
        override val onActionClick: () -> Unit,
        override val onPolymarketTermsClick: () -> Unit,
        override val onTangemTermsClick: () -> Unit,
        val isRegionRestrictionsShown: Boolean,
        val onRegionRestrictionsDismiss: () -> Unit,
    ) : Action {

        override fun copyInProgress(isInProgress: Boolean): Action = copy(isInProgress = isInProgress)
    }

    /**
     * The account exists and only this device is unlinked. No region check and no consent belong here: the run
     * neither deploys nor asks for anything new, so there is nothing to restrict and nothing to agree to.
     */
    data class Reconnect(
        override val isInProgress: Boolean,
        override val actionButtonText: TextReference,
        override val onActionClick: () -> Unit,
        override val onPolymarketTermsClick: () -> Unit,
        override val onTangemTermsClick: () -> Unit,
    ) : Action {

        override fun copyInProgress(isInProgress: Boolean): Action = copy(isInProgress = isInProgress)
    }
}