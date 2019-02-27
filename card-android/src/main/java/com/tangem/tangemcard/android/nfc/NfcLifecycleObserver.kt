package com.tangem.tangemcard.android.nfc

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.tangem.tangemcard.android.reader.NfcManager

/**
 * Lifecycle observer for all activities and fragments with nfc
 *
 * @see NfcManager
 */
class NfcLifecycleObserver(private var nfcManager: NfcManager) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        nfcManager.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        nfcManager.onPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        nfcManager.onStop()
    }

}