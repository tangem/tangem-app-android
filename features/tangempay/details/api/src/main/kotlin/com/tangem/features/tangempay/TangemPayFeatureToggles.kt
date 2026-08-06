package com.tangem.features.tangempay

interface TangemPayFeatureToggles {
    val isRemoveAccountEnabled: Boolean
    val isTiersPlusPlanEnabled: Boolean
    val isCashbackEnabled: Boolean
    val isPlasticCardOrderEnabled: Boolean
    val isAccountMultichainEnabled: Boolean
    val isPinBiometryGateEnabled: Boolean
}