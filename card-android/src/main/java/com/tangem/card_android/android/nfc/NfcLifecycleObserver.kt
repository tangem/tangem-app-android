package com.tangem.card_android.android.nfc

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.tangem.card_android.android.reader.NfcManager

/**
 * Lifecycle observer for all activities and fragments with nfc
 *
 * @see NfcManager
 */
class NfcLifecycleObserver(private var nfcManager: NfcManager) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        nfcManager.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        nfcManager.onPause()
        nfcManager.onStop()
    }

}