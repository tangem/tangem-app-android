package com.tangem.tangem_sdk_new

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.tangem.tangem_sdk_new.nfc.NfcManager

/**
 * [LifecycleObserver] for [NfcManager], helps to coordinate NFC modes with Activity lifecycle.
 */
class NfcLifecycleObserver(private var nfcManager: NfcManager) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume(owner: LifecycleOwner) {
        nfcManager.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause(owner: LifecycleOwner) {
        nfcManager.onPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy(owner: LifecycleOwner) {
        nfcManager.onDestroy()
    }
}