package com.tangem.tangem_sdk_new.extensions

import androidx.fragment.app.FragmentActivity
import com.tangem.Config
import com.tangem.SessionViewDelegate
import com.tangem.TangemSdk
import com.tangem.tangem_sdk_new.DefaultSessionViewDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.TerminalKeysStorage
import com.tangem.tangem_sdk_new.nfc.NfcManager

fun TangemSdk.Companion.init(activity: FragmentActivity, config: Config = Config()): TangemSdk {
    val nfcManager = TangemSdk.initNfcManager(activity)

    val viewDelegate = DefaultSessionViewDelegate(nfcManager.reader)
    viewDelegate.activity = activity

    val tangemSdk = TangemSdk(nfcManager.reader, viewDelegate, config)
    tangemSdk.setTerminalKeysService(TerminalKeysStorage(activity.application))

    return tangemSdk
}

fun TangemSdk.Companion.customInit(
        activity: FragmentActivity,
        viewDelegate: SessionViewDelegate? = null,
        config: Config = Config()
): TangemSdk {
    val nfcManager = TangemSdk.initNfcManager(activity)

    val tangemSdk = TangemSdk(
            nfcManager.reader,
            viewDelegate ?: DefaultSessionViewDelegate(nfcManager.reader)
                    .apply { this.activity = activity },
            config
    )
    tangemSdk.setTerminalKeysService(TerminalKeysStorage(activity.application))

    return tangemSdk
}

fun TangemSdk.Companion.initNfcManager(activity: FragmentActivity): NfcManager {
    val nfcManager = NfcManager()
    nfcManager.setCurrentActivity(activity)
    activity.lifecycle.addObserver(NfcLifecycleObserver(nfcManager))
    return nfcManager
}