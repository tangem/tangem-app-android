package com.tangem.feature.tester.presentation.menu.state

import androidx.annotation.StringRes
import com.tangem.feature.tester.impl.R
import kotlinx.collections.immutable.ImmutableSet

/**
 * Content state of tester menu screen
 *
 * @property onBackClick   the lambda to be invoked when back button is pressed
 * @property buttons       list of buttons
 * @property onButtonClick the lambda to be invoked when tester button is pressed
 */
data class TesterMenuUM(
    val onBackClick: () -> Unit,
    val buttons: ImmutableSet<ButtonUM>,
    val onButtonClick: (ButtonUM) -> Unit,
) {

    enum class ButtonUM(@StringRes val textResId: Int) {
        FEATURE_TOGGLES(R.string.feature_toggles),
        EXCLUDED_BLOCKCHAINS(R.string.excluded_blockchains),
        ENVIRONMENT_TOGGLES(R.string.environment_toggles),
        BLOCKCHAIN_PROVIDERS(R.string.blockchain_providers),
        TESTER_ACTIONS(R.string.tester_actions),
        ;
    }
}