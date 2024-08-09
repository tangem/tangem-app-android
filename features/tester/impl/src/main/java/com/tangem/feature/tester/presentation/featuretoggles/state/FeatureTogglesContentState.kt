package com.tangem.feature.tester.presentation.featuretoggles.state

import com.tangem.feature.tester.presentation.featuretoggles.models.TesterFeatureToggle
import kotlinx.collections.immutable.ImmutableList

/**
 * Content state of feature toggles screen
 *
 * @property featureToggles      feature toggles list
 * @property onBackClick         the lambda to be invoked when back button is pressed
 * @property onToggleValueChange the lambda to be invoked when switch button is pressed
 * @property onApplyChangesClick the lambda to be invoked when apply changes button is pressed
 */
internal data class FeatureTogglesContentState(
    val featureToggles: ImmutableList<TesterFeatureToggle>,
    val onToggleValueChange: (String, Boolean) -> Unit,
    val onBackClick: () -> Unit,
    val onApplyChangesClick: () -> Unit,
)