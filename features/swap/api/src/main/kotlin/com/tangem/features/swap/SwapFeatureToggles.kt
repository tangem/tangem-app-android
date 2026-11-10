package com.tangem.features.swap

interface SwapFeatureToggles {
    val isTronGaslessDexSwapEnabled: Boolean
    val isEvmGaslessDexSwapEnabled: Boolean
    val isTronClientEnergyEnabled: Boolean
    val isTronLowerFeeTypeEnabled: Boolean
    val isSolanaGaslessEnabled: Boolean
}