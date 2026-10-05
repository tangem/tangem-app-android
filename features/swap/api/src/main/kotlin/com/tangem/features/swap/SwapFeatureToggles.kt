package com.tangem.features.swap

interface SwapFeatureToggles {
    val isYieldDexTransferEnabled: Boolean
    val isChooseTokenPulseEnabled: Boolean
    val isExpressCategoriesGeoBlockingEnabled: Boolean
    val isTronGaslessSwapEnabled: Boolean
}