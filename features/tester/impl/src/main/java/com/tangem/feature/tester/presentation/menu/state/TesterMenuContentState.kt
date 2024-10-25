package com.tangem.feature.tester.presentation.menu.state

/**
 * Content state of tester menu screen
 *
 * @property onBackClick               the lambda to be invoked when back button is pressed
 * @property onFeatureTogglesClick     the lambda to be invoked when feature toggles button is pressed
 * @property onEnvironmentTogglesClick the lambda to be invoked when environment toggles button is pressed
 * @property onTesterActionsClick      the lambda to be invoked when tester actions button is pressed
 */
data class TesterMenuContentState(
    val onBackClick: () -> Unit,
    val onFeatureTogglesClick: () -> Unit,
    val onEnvironmentTogglesClick: () -> Unit,
    val onExcludedBlockchainsClick: () -> Unit,
    val onTesterActionsClick: () -> Unit,
)