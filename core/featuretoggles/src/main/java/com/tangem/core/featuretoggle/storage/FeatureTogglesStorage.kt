package com.tangem.core.featuretoggle.storage

import com.tangem.core.featuretoggle.models.FeatureToggleDTO

/**
 * Component that initializes and stores a list of feature toggles
 *
 * @author Andrew Khokhlov on 25/01/2023
 */
internal interface FeatureTogglesStorage {

    /** List of feature toggles */
    val featureToggles: List<FeatureToggleDTO>

    /** Initialize storage */
    suspend fun init()
}
