package com.tangem.ui.activity

import androidx.lifecycle.ViewModel
import com.tangem.ui.navigation.NavigationResult

class GlobalViewModel : ViewModel() {
    var navigationResult: NavigationResult? = null
}