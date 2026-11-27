package com.tangem.features.swap

interface SwapFeatureToggles {
    val isTronGaslessDexSwapEnabled: Boolean
    val isTronClientEnergyEnabled: Boolean
    val isTronLowerFeeTypeEnabled: Boolean
    val isSolanaGaslessEnabled: Boolean
    val isSolanaGaslessRentEnabled: Boolean
}