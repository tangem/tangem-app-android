package com.tangem.feature.tester.presentation.featuretoggles.state

import com.tangem.feature.tester.presentation.featuretoggles.models.TesterFeatureToggle

/**
 * State holder of feature toggles screen
 *
[REDACTED_AUTHOR]
 */
sealed interface FeatureTogglesStateHolder {

    /**
     * Content
     *
     * @property featureToggles      feature toggles list
     * @property onToggleValueChange the lambda to be invoked when switch button is pressed
     * @property onBackClicked       the lambda to be invoked when back button is pressed
     */
    data class Content(
        val onBackClicked: () -> Unit,
        val featureToggles: List<TesterFeatureToggle>,
        val onToggleValueChange: (String, Boolean) -> Unit,
    ) : FeatureTogglesStateHolder
}