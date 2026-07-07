package com.tangem.feature.tester.presentation.featuretoggles.state

import kotlinx.collections.immutable.ImmutableList

/**
 * Group of feature toggles sharing the same release [status]
 *
 * @property status  release status common for all [toggles]
 * @property toggles toggles of this group
 */
internal data class FeatureToggleGroupUM(
    val status: TesterFeatureToggleUM.Status,
    val toggles: ImmutableList<TesterFeatureToggleUM>,
)