package com.tangem.features.swap.v2.impl.common

import com.tangem.features.swap.v2.impl.common.entity.NavigationUM

internal interface SwapNavigationModelCallback {
    fun onNavigationResult(navigationUM: NavigationUM)
}