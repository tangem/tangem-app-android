package com.tangem.features.swap

interface SwapFeatureToggles {
    val isYieldSwapEnabled: Boolean
    val isSwapSwitchToTransferEnabled: Boolean
    val isSwapIntegratedApproveEnabled: Boolean
    val isExpressShareButtonEnabled: Boolean
    val isSwapBestDexRateEnabled: Boolean
    val isHighFeeWarningEnabled: Boolean
}