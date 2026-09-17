package com.tangem.features.tangempay

interface TangemPayFeatureToggles {
    val isCashbackEnabled: Boolean
    val isPlasticCardOrderEnabled: Boolean
    val isPinBiometryGateEnabled: Boolean
    val isBankOfframpEnabled: Boolean
    val isCustomCardsCobrandEnabled: Boolean
    val isResidencyStepEnabled: Boolean
}