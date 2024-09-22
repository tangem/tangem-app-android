package com.tangem.feature.tester.presentation.featuretoggles.state

import com.tangem.feature.tester.presentation.featuretoggles.models.TesterFeatureToggle
import kotlinx.collections.immutable.ImmutableList

/**
 * Content state of feature toggles screen
 *
 * @property topBarState         top bar state
 * @property appVersion          app version
 * @property featureToggles      feature toggles list
 * @property onBackClick         the lambda to be invoked when back button is pressed
 * @property onToggleValueChange the lambda to be invoked when switch button is pressed
 * @property onRestartAppClick   the lambda to be invoked when restart app button is pressed
 */
internal data class FeatureTogglesContentState(
    val topBarState: TopBarState,
    val appVersion: String,
    val featureToggles: ImmutableList<TesterFeatureToggle>,
    val onBackClick: () -> Unit,
    val onToggleValueChange: (String, Boolean) -> Unit,
    val onRestartAppClick: () -> Unit,
) {

    /** Top bar state */
    sealed interface TopBarState {

        /**
         * Config setup
         *
         * @property onBackClick the lambda to be invoked when back button is pressed
         */
        data object ConfigSetup : TopBarState

        /**
         * Custom setup
         *
         * @property onRecoverClick the lambda to be invoked when recover button is pressed
         */
        data class CustomSetup(val onRecoverClick: () -> Unit) : TopBarState
    }
}
