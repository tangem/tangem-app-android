package com.tangem.data.cloudbackup.datasource

import arrow.core.Either
import com.tangem.domain.cloudbackup.models.CloudBackupError

/**
 * Drives the interactive Google authorization (account picker + consent) and yields an access token.
 *
 * The data layer depends only on this contract; the concrete implementation lives in the app layer
 * because it needs an `Activity`/`ActivityResultLauncher` to launch the consent resolution.
 */
interface GoogleDriveAuthorizer {

    suspend fun authorize(interactive: Boolean = true): Either<CloudBackupError, GoogleDriveAuthResult>

    fun clearAuthorization()

    suspend fun clearToken(token: String)
}

data class GoogleDriveAuthResult(
    val accessToken: String,
)