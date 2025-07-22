package com.tangem.features.swap.v2.impl.common

import com.tangem.common.ui.navigationButtons.NavigationUM

internal interface SwapNavigationModelCallback {
    fun onNavigationResult(navigationUM: NavigationUM)
    fun onBackClick()
    fun onNextClick()
}