package com.tangem.feature.tester.presentation.featuretoggles.state

import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Content state of feature toggles screen
 *
 * @property topBar              top bar state
 * @property appVersion          app version
 * @property searchBar           search bar state used to filter [featureToggleGroups] by toggle name
 * @property featureToggleGroups feature toggles grouped by release status
 * @property onToggleValueChange the lambda to be invoked when switch button is pressed
 * @property onRestartAppClick   the lambda to be invoked when restart app button is pressed
 */
internal data class FeatureTogglesScreenUM(
    val topBar: TopBarWithRefreshUM,
    val appVersion: String,
    val searchBar: TangemSearch.State,
    val featureToggleGroups: ImmutableList<FeatureToggleGroupUM>,
    val onToggleValueChange: (String, Boolean) -> Unit,
    val onRestartAppClick: () -> Unit,
)