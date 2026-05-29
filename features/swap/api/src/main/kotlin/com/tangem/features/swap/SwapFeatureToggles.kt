package com.tangem.features.swap

interface SwapFeatureToggles {
    val isYieldSwapEnabled: Boolean
    val isSwapSwitchToTransferEnabled: Boolean
    val isSwapIntegratedApproveEnabled: Boolean
    val isSwapAbEnabled: Boolean
    val isSwapProviderFilterEnabled: Boolean
    val isSwapRateExperienceEnabled: Boolean
    val isSwapPredefinedButtonsEnabled: Boolean
}