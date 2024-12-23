package com.tangem.sdk.api

import androidx.activity.ComponentActivity
import com.tangem.TangemSdk
import com.tangem.operations.backup.BackupService
import com.tangem.sdk.extensions.init
import java.lang.ref.WeakReference

class BackupServiceHolder {

    lateinit var backupService: WeakReference<BackupService>
        private set

    fun createAndSetService(tangemSdk: TangemSdk, activity: ComponentActivity) {
        backupService = WeakReference(BackupService.init(tangemSdk, activity))
    }
}