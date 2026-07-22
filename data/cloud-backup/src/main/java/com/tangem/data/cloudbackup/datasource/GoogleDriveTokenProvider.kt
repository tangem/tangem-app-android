package com.tangem.data.cloudbackup.datasource

import arrow.core.Either
import com.tangem.domain.cloudbackup.models.CloudBackupError

/**
 * Provides an OAuth access token with the Google Drive scope.
 *
 * The real implementation is backed by Google Identity `AuthorizationClient` and requires
 * user interaction (account picker + consent), which is driven by the feature layer.
 */
interface GoogleDriveTokenProvider {

    suspend fun getAccessToken(interactive: Boolean = true): Either<CloudBackupError, String>

    suspend fun invalidate()

    suspend fun signOut()
}