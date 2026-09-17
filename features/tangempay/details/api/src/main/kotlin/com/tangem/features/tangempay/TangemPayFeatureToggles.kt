package com.tangem.features.tangempay

interface TangemPayFeatureToggles {
    val isTiersPlusPlanEnabled: Boolean
    val isCashbackEnabled: Boolean
    val isPlasticCardOrderEnabled: Boolean
    val isBankOfframpEnabled: Boolean
    val isCustomCardsCobrandEnabled: Boolean
    val isResidencyStepEnabled: Boolean
}