package com.tangem.tap.features.customtoken.api.featuretoggles

/**
 * Add custom token feature toggles
 *
[REDACTED_AUTHOR]
 */
interface CustomTokenFeatureToggles {

    /** Availability of redesigned screen (internal feature) */
    val isRedesignedScreenEnabled: Boolean

    val isNewCardScanningEnabled: Boolean
}