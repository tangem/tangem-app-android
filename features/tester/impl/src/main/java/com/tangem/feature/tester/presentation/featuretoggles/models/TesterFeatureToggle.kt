package com.tangem.feature.tester.presentation.featuretoggles.models

/**
 * Presentation model of feature toggle
 *
 * @property name      name
 * @property isEnabled availability
 *
 * @author Andrew Khokhlov on 07/02/2023
 */
internal data class TesterFeatureToggle(
    val name: String,
    val isEnabled: Boolean,
)
