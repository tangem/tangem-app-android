package com.tangem.features.swap

interface SwapFeatureToggles {
    val isYieldSwapEnabled: Boolean
    val isHighFeeWarningEnabled: Boolean
    val isTronDexSwapEnabled: Boolean
}