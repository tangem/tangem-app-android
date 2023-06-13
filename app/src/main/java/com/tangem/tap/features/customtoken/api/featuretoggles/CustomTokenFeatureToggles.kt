package com.tangem.tap.features.customtoken.api.featuretoggles

/**
 * Add custom token feature toggles
 *
 * @author Andrew Khokhlov on 04/04/2023
 */
interface CustomTokenFeatureToggles {

    /** Availability of redesigned screen (internal feature) */
    val isRedesignedScreenEnabled: Boolean

    val isNewCardScanningEnabled: Boolean
}
