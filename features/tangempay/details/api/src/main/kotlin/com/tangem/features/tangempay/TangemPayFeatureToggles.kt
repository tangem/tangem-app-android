package com.tangem.features.tangempay

interface TangemPayFeatureToggles {
    val isTiersPlusPlanEnabled: Boolean
    val isCashbackEnabled: Boolean
    val isPlasticCardOrderEnabled: Boolean
    val isAccountMultichainEnabled: Boolean
    val isPinBiometryGateEnabled: Boolean
}