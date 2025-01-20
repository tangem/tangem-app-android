package com.tangem.feature.tester.presentation.featuretoggles.state

import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import com.tangem.feature.tester.presentation.featuretoggles.models.TesterFeatureToggle
import kotlinx.collections.immutable.ImmutableList

/**
 * Content state of feature toggles screen
 *
 * @property topBar            top bar state
 * @property appVersion          app version
 * @property featureToggles      feature toggles list
 * @property onToggleValueChange the lambda to be invoked when switch button is pressed
 * @property onRestartAppClick   the lambda to be invoked when restart app button is pressed
 */
internal data class FeatureTogglesContentState(
    val topBar: TopBarWithRefreshUM,
    val appVersion: String,
    val featureToggles: ImmutableList<TesterFeatureToggle>,
    val onToggleValueChange: (String, Boolean) -> Unit,
    val onRestartAppClick: () -> Unit,
)