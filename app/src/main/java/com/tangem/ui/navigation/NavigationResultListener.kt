package com.tangem.ui.navigation

import android.os.Bundle

interface NavigationResultListener {
    fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle? = null)
}