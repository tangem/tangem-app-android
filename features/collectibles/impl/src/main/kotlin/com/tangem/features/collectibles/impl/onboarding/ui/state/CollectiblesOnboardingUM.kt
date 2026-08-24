package com.tangem.features.collectibles.impl.onboarding.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * State of the Collectibles Onboarding screen.
 *
 * Carries the marketing copy as well as the runtime flags: the copy is still hardcoded, so keeping it in the
 * state means the screen does not have to change when it moves to Lokalise.
 *
 * @property title headline above the benefit cards.
 * @property subtitle supporting paragraph under the headline.
 * @property benefits selling points, laid out two per row.
 * @property faq always-expanded question-and-answer blocks, in display order.
 * @property collectiblesTitle link title of the service's terms, spliced into the legal line above the CTA.
 * @property tangemTitle link title of the Tangem terms, spliced into the same legal line.
 * @property createAccountButtonText label of the CTA, hidden while [isCreatingAccount] is true.
 * @property isCreatingAccount drives the CTA's loader; while `true` the button ignores further taps.
 * @property onCloseClick dismisses the screen from the top navigation.
 * @property onCreateAccountClick starts account creation.
 * @property onCollectiblesTermsClick opens the service's terms.
 * @property onTangemTermsClick opens the Tangem terms.
 */
@Immutable
internal data class CollectiblesOnboardingUM(
    val title: TextReference,
    val subtitle: TextReference,
    val benefits: ImmutableList<Benefit>,
    val faq: ImmutableList<FaqEntry>,
    val collectiblesTitle: TextReference,
    val tangemTitle: TextReference,
    val createAccountButtonText: TextReference,
    val isCreatingAccount: Boolean,
    val onCloseClick: () -> Unit,
    val onCreateAccountClick: () -> Unit,
    val onCollectiblesTermsClick: () -> Unit,
    val onTangemTermsClick: () -> Unit,
) {

    data class Benefit(val icon: ImageVector, val title: TextReference, val subtitle: TextReference)

    data class FaqEntry(val question: TextReference, val answer: TextReference)
}