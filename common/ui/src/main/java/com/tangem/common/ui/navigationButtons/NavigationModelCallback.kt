package com.tangem.common.ui.navigationButtons

interface NavigationModelCallback {
    fun onNavigationResult(navigationUM: NavigationUM)
    fun onBackClick()
    fun onNextClick()
}