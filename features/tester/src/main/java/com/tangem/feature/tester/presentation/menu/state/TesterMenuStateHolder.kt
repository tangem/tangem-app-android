package com.tangem.feature.tester.presentation.menu.state

/**
 * State holder of tester menu screen
 *
 * @author Andrew Khokhlov on 07/02/2023
 */
sealed interface TesterMenuStateHolder {

    /**
     * Content
     *
     * @property onBackClicked           the lambda to be invoked when back button is pressed
     * @property onFeatureTogglesClicked the lambda to be invoked when feature toggles button is pressed
     */
    data class Content(
        val onBackClicked: () -> Unit,
        val onFeatureTogglesClicked: () -> Unit
    ) : TesterMenuStateHolder
}
