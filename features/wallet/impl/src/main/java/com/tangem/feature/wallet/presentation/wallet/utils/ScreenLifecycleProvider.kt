package com.tangem.feature.wallet.presentation.wallet.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ViewModelScoped
internal class ScreenLifecycleProvider @Inject constructor() : DefaultLifecycleObserver {

    private val _isBackgroundState = MutableStateFlow(false)
    val isBackgroundState: StateFlow<Boolean> = _isBackgroundState

    override fun onResume(owner: LifecycleOwner) {
        _isBackgroundState.value = false
    }

    override fun onPause(owner: LifecycleOwner) {
        _isBackgroundState.value = true
    }
}