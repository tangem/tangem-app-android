package com.tangem.features.earn.api.ui

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * The "How it works" sheet of an Earn product: numbered [steps], then the [footnote]. [onLearnMoreClick] adds a
 * "Learn more" button next to "Got it".
 */
data class EarnHowItWorksUM(
    val title: TextReference,
    val steps: ImmutableList<Step>,
    val footnote: TextReference?,
    val onLearnMoreClick: (() -> Unit)?,
) : TangemBottomSheetConfigContent {

    data class Step(val title: TextReference, val description: TextReference)
}