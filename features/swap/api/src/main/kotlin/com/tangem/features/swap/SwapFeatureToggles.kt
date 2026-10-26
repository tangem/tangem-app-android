package com.tangem.features.swap

interface SwapFeatureToggles {
    val isYieldDexTransferEnabled: Boolean
    val isTronGaslessSwapEnabled: Boolean
    val isTronGaslessDexSwapEnabled: Boolean
    val isTronClientEnergyEnabled: Boolean
    val isTronLowerFeeTypeEnabled: Boolean
    val isCustomSlippageEnabled: Boolean
    val isOverallBestPriceEnabled: Boolean
}