package com.tangem.core.featuretoggle.storage

/**
 * Component that initializes and stores a list of feature toggles
 *
 * @author Andrew Khokhlov on 25/01/2023
 */
internal interface FeatureTogglesStorage {

    /** List of feature toggles */
    val featureToggles: List<FeatureToggle>

    /** Initialize storage */
    suspend fun init()
}
