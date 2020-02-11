package com.tangem.tangem_sdk_new.extensions

import androidx.fragment.app.FragmentActivity
import com.tangem.CardManager
import com.tangem.tangem_sdk_new.DefaultCardManagerDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.TerminalKeysStorage
import com.tangem.tangem_sdk_new.nfc.NfcManager

fun CardManager.Companion.init(activity: FragmentActivity): CardManager {
    val nfcManager = NfcManager().apply {
        this.setCurrentActivity(activity)
        activity.lifecycle.addObserver(NfcLifecycleObserver(this))
    }
    val cardManagerDelegate = DefaultCardManagerDelegate(nfcManager.reader).apply {
        this.activity = activity
    }
    return CardManager(nfcManager.reader, cardManagerDelegate).apply {
        this.setTerminalKeysService(TerminalKeysStorage(activity.application))
    }
}