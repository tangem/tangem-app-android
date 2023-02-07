package com.tangem.feature.tester.presentation.featuretoggles.models

/**
 * Presentation model of feature toggle
 *
 * @property name      name
 * @property isEnabled availability
 *
[REDACTED_AUTHOR]
 */
data class TesterFeatureToggle(
    val name: String,
    val isEnabled: Boolean
)