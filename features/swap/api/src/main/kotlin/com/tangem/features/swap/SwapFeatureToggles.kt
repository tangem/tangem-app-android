package com.tangem.features.swap

interface SwapFeatureToggles {
    val isTronGaslessDexSwapEnabled: Boolean
    val isEvmGaslessDexSwapEnabled: Boolean
    val isOverallBestPriceEnabled: Boolean
    val isHideRejectedCexProviderEnabled: Boolean
}