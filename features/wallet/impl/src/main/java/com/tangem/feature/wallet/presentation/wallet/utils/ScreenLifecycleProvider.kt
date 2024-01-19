package com.tangem.feature.wallet.presentation.wallet.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
internal class ScreenLifecycleProvider @Inject constructor() : DefaultLifecycleObserver {

    var isBackground: Boolean = true
        private set

    override fun onResume(owner: LifecycleOwner) {
        isBackground = false
    }

    override fun onPause(owner: LifecycleOwner) {
        isBackground = true
    }
}