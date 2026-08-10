package com.tangem.features.swap

interface SwapFeatureToggles {
    val isYieldDexTransferEnabled: Boolean
    val isTronDexSwapEnabled: Boolean
    val isChooseTokenPulseEnabled: Boolean
    val isHideZeroBalanceSourceEnabled: Boolean
    val isSwapDeeplinkEnabled: Boolean
}