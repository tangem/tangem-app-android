package com.tangem.google.drive

import arrow.core.Either
import com.tangem.google.auth.GoogleAuthError

/**
 * Provides an OAuth access token with the Google Drive `drive.file` scope, shared by every feature
 * that stores files in the user's Drive.
 *
 * Errors stay generic ([GoogleAuthError]) — each feature maps them to its own domain errors.
 */
interface GoogleDriveTokenProvider {

    /**
     * @param interactive when `true`, may show the account picker and the consent screen; when `false`,
     * resolves only from an already-granted session and yields [GoogleAuthError.AuthRequired] otherwise.
     */
    suspend fun getAccessToken(interactive: Boolean = true): Either<GoogleAuthError, String>

    /** Drops the cached token and the authorization state, so the next call authorizes from scratch. */
    suspend fun invalidate()

    /**
     * [invalidate], then pins the following authorizations to [accountName] — one operation, because in
     * the gap between two a concurrent [getAccessToken] can cache a token for the account being left.
     */
    suspend fun switchAccount(accountName: String)

    /** Same as [invalidate], but also revokes the token remotely. */
    suspend fun signOut()
}