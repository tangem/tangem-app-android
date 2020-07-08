package com.tangem.tangem_sdk_new.extensions

import androidx.activity.ComponentActivity
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.tangem.Config
import com.tangem.Database
import com.tangem.SessionViewDelegate
import com.tangem.TangemSdk
import com.tangem.common.CardValuesDbStorage
import com.tangem.tangem_sdk_new.DefaultSessionViewDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.TerminalKeysStorage
import com.tangem.tangem_sdk_new.nfc.NfcManager

fun TangemSdk.Companion.init(activity: ComponentActivity, config: Config = Config()): TangemSdk {
    val nfcManager = TangemSdk.initNfcManager(activity)

    val viewDelegate = DefaultSessionViewDelegate(nfcManager.reader)
    viewDelegate.activity = activity

    val databaseDriver = AndroidSqliteDriver(Database.Schema, activity.applicationContext, "cards.db")
    return TangemSdk(
            nfcManager.reader, viewDelegate, config,
            CardValuesDbStorage(databaseDriver), TerminalKeysStorage(activity.application)
    )
}

fun TangemSdk.Companion.customInit(
        activity: ComponentActivity,
        viewDelegate: SessionViewDelegate? = null,
        config: Config = Config()
): TangemSdk {
    val nfcManager = TangemSdk.initNfcManager(activity)
    val databaseDriver = AndroidSqliteDriver(Database.Schema, activity.applicationContext, "cards.db")

    return TangemSdk(
            nfcManager.reader,
            viewDelegate ?: DefaultSessionViewDelegate(nfcManager.reader)
                    .apply { this.activity = activity },
            config,
            CardValuesDbStorage(databaseDriver), TerminalKeysStorage(activity.application)
    )
}

fun TangemSdk.Companion.initNfcManager(activity: ComponentActivity): NfcManager {
    val nfcManager = NfcManager()
    nfcManager.setCurrentActivity(activity)
    activity.lifecycle.addObserver(NfcLifecycleObserver(nfcManager))
    return nfcManager
}
