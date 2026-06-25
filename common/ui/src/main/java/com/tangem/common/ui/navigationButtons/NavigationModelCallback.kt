package com.tangem.common.ui.navigationButtons

import com.tangem.core.decompose.navigation.Route

interface NavigationModelCallback {
    fun onBackClick(currentRoute: Route)
    fun onNextClick(currentRoute: Route)
}