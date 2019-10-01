package com.tangem.tangem_sdk_new

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner


class NfcLifecycleObserver(private var nfcManager: NfcManager) : DefaultLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        nfcManager.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        nfcManager.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        nfcManager.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        nfcManager.onDestroy()
    }
}