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

    suspend fun currentAccountName(): String?

    /**
     * @param interactive when `true`, may show the account picker and the consent screen; when `false`,
     * resolves only from an already-granted session and yields [GoogleAuthError.AuthRequired] otherwise.
     */
    suspend fun getAccessToken(interactive: Boolean = true): Either<GoogleAuthError, String>

    /** Drops the cached tokens and the authorization state, so the next call authorizes from scratch. */
    suspend fun invalidate()

    /**
     * Drops only the cached token and keeps the pinned account. For a token the server rejected: the
     * re-authorization must stay in the same account, while [invalidate] would let it pick another one.
     */
    suspend fun invalidateToken()

    /** `null` leaves the choice of the account to Google */
    suspend fun switchAccount(accountName: String?)
}