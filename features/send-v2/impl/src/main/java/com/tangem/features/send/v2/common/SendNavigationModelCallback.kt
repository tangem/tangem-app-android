package com.tangem.features.send.v2.common

import com.tangem.features.send.v2.common.ui.state.NavigationUM

internal interface SendNavigationModelCallback {
    fun onNavigationResult(navigationUM: NavigationUM)
}