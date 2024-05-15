package com.tangem.features.send.api.featuretoggles

/**
 * Send feature toggles
 */
interface SendFeatureToggles {

    /** Availability of redesigned send screen */
    val isRedesignedSendEnabled: Boolean

    /** Updates remote toggle */
    suspend fun fetchNewSendEnabled()
}
