package com.tangem.feature.tester.presentation.featuretoggles.state

import com.tangem.feature.tester.presentation.featuretoggles.models.TesterFeatureToggle

/**
 * Content state of feature toggles screen
 *
 * @property featureToggles      feature toggles list
 * @property onBackClick         the lambda to be invoked when back button is pressed
 * @property onToggleValueChange the lambda to be invoked when switch button is pressed
 */
internal data class FeatureTogglesContentState(
    val featureToggles: List<TesterFeatureToggle>,
    val onBackClick: () -> Unit,
    val onToggleValueChange: (String, Boolean) -> Unit,
)
