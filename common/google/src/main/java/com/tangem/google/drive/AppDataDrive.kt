package com.tangem.google.drive

import javax.inject.Qualifier

/**
 * The [GoogleDriveTokenProvider] scoped to the hidden `appDataFolder`; the unqualified binding stays on
 * `drive.file`, so a feature never silently gets a token for a scope it did not ask the user to grant.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppDataDrive