package com.tangem.feature.wallet.presentation.wallet.utils

import com.arkivanov.essenty.lifecycle.Lifecycle
import com.tangem.core.decompose.di.ModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class ScreenLifecycleProvider @Inject constructor() : Lifecycle.Callbacks {

    private val _isBackgroundState = MutableStateFlow(false)
    val isBackgroundState: StateFlow<Boolean> = _isBackgroundState

    override fun onResume() {
        _isBackgroundState.value = false
    }

    override fun onPause() {
        _isBackgroundState.value = true
    }
}