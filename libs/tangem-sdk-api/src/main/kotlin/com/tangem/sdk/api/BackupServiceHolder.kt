package com.tangem.sdk.api

import com.tangem.operations.backup.BackupService

interface BackupServiceHolder {

    /**

     * with the SDK when the activity is destroyed, so don't keep it beyond the current screen.
     */
    val backupService: BackupService
}