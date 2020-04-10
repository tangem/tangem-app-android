package com.tangem.tangem_sdk_new.extensions

import androidx.fragment.app.FragmentActivity
import com.tangem.TangemSdk
import com.tangem.tangem_sdk_new.DefaultSessionViewDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.TerminalKeysStorage
import com.tangem.tangem_sdk_new.nfc.NfcManager

fun TangemSdk.Companion.init(activity: FragmentActivity): TangemSdk {
    val nfcManager = NfcManager().apply {
        this.setCurrentActivity(activity)
        activity.lifecycle.addObserver(NfcLifecycleObserver(this))
    }
    val viewDelegate = DefaultSessionViewDelegate(nfcManager.reader).apply {
        this.activity = activity
    }
    return TangemSdk(nfcManager.reader, viewDelegate).apply {
        this.setTerminalKeysService(TerminalKeysStorage(activity.application))
    }
}