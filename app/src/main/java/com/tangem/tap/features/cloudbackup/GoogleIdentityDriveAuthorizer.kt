package com.tangem.tap.features.cloudbackup

import arrow.core.Either
import com.tangem.data.cloudbackup.datasource.GoogleDriveAuthResult
import com.tangem.data.cloudbackup.datasource.GoogleDriveAuthorizer
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.google.auth.GoogleAuthError
import com.tangem.google.auth.GoogleAuthScope
import com.tangem.google.auth.GoogleAuthorizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drive-specific [GoogleDriveAuthorizer] over the reusable [GoogleAuthorizer] core: requests only the

 * the generic [GoogleAuthError] to [CloudBackupError].
 */
@Singleton
internal class GoogleIdentityDriveAuthorizer @Inject constructor(
    private val googleAuthorizer: GoogleAuthorizer,
) : GoogleDriveAuthorizer {

    override suspend fun authorize(interactive: Boolean): Either<CloudBackupError, GoogleDriveAuthResult> {
        return googleAuthorizer.authorize(scopes = listOf(DRIVE_FILE_SCOPE), interactive = interactive)
            .mapLeft { it.toCloudBackupError() }
            .map { GoogleDriveAuthResult(accessToken = it.accessToken) }
    }

    override fun clearAuthorization() {
        googleAuthorizer.clearAuthorization()
    }

    override suspend fun clearToken(token: String) {
        googleAuthorizer.clearToken(token)
    }

    private fun GoogleAuthError.toCloudBackupError(): CloudBackupError = when (this) {
        GoogleAuthError.AuthCanceled -> CloudBackupError.AuthCanceled
        GoogleAuthError.AuthRequired -> CloudBackupError.AuthRequired
        GoogleAuthError.PermissionsMissing -> CloudBackupError.AuthPermissionsMissing
        GoogleAuthError.Unavailable -> CloudBackupError.CloudUnavailable
        GoogleAuthError.NetworkError -> CloudBackupError.NetworkError
        is GoogleAuthError.Unknown -> CloudBackupError.Unknown(cause)
    }

    private companion object {
        val DRIVE_FILE_SCOPE = GoogleAuthScope("https://www.googleapis.com/auth/drive.file")
    }
}