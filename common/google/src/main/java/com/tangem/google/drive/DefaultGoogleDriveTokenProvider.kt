package com.tangem.google.drive

import android.content.Context
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.local.connection.NetworkConnectionManager
import com.tangem.google.GoogleServicesHelper
import com.tangem.google.auth.GoogleAuthError
import com.tangem.google.auth.GoogleAuthScope
import com.tangem.google.auth.GoogleAuthorizer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default [GoogleDriveTokenProvider]:
 * 1. bails out with [GoogleAuthError.Unavailable] when Google Play services are missing;
 * 2. serves the session-cached token without any user interaction;
 * 3. bails out with [GoogleAuthError.NetworkError] when the device is offline;
 * 4. on a cache miss, delegates to [GoogleAuthorizer] (honoring `interactive`) and caches the result.
 *
 * Only the [scopes] the caller asked for are requested, so Tangem sees nothing else of the user's

 * scope set gets its own instance, so a token granted for one is never served to the other.
 *
 * The 401/403 → re-auth path belongs to the callers, which invalidate the token and retry;
 * this provider intentionally exposes nothing extra.
 */
internal class DefaultGoogleDriveTokenProvider(
    private val authorizer: GoogleAuthorizer,
    private val context: Context,
    private val networkConnectionManager: NetworkConnectionManager,
    private val scopes: List<GoogleAuthScope> = listOf(DRIVE_FILE_SCOPE),
) : GoogleDriveTokenProvider {

    private val authMutex = Mutex()

    private val cachedTokens = mutableMapOf<String?, String>()

    override suspend fun currentAccountName(): String? = currentCoroutineContext()[GoogleDriveAccount].accountName()

    override suspend fun getAccessToken(interactive: Boolean): Either<GoogleAuthError, String> {
        if (!GoogleServicesHelper.checkGoogleServicesAvailability(context)) {
            return GoogleAuthError.Unavailable.left()
        }
        val requested = currentCoroutineContext()[GoogleDriveAccount]

        // serialize authorization so two concurrent cache misses don't open two account pickers
        return authMutex.withLock {
            // a token cached under `null` belongs to whichever account Google picked, not the one about to be chosen
            val isAccountPickPending = interactive && requested == null && authorizer.selectedAccountName == null
            if (!isAccountPickPending) cachedTokens[requested.accountName()]?.let { return@withLock it.right() }
            if (!networkConnectionManager.isOnline) return@withLock GoogleAuthError.NetworkError.left()

            val result = if (requested != null) {
                authorizer.authorizeAccount(accountName = requested.name, scopes = scopes, interactive = interactive)
            } else {
                authorizer.authorize(scopes = scopes, interactive = interactive)
            }
            result.map { authResult ->
                // read only now: the account picker may have just pinned the account
                cachedTokens[requested.accountName()] = authResult.accessToken
                authResult.accessToken
            }
        }
    }

    override suspend fun invalidate() {
        authMutex.withLock {
            cachedTokens.values.forEach { authorizer.clearToken(it) }
            cachedTokens.clear()
            authorizer.clearAuthorization()
        }
    }

    override suspend fun invalidateToken() {
        val requested = currentCoroutineContext()[GoogleDriveAccount]
        authMutex.withLock {
            cachedTokens.remove(requested.accountName())?.let { authorizer.clearToken(it) }
        }
    }

    override suspend fun switchAccount(accountName: String?) {
        authMutex.withLock {
            if (authorizer.selectedAccountName == accountName) return@withLock
            authorizer.clearAuthorization()
            accountName?.let(authorizer::selectAccount)
        }
    }

    private fun GoogleDriveAccount?.accountName(): String? = if (this != null) name else authorizer.selectedAccountName

    companion object {
        val DRIVE_FILE_SCOPE = GoogleAuthScope("https://www.googleapis.com/auth/drive.file")

        /** Grants access to the hidden `appDataFolder` only — nothing the user can see in their Drive */
        val DRIVE_APP_DATA_SCOPE = GoogleAuthScope("https://www.googleapis.com/auth/drive.appdata")
    }
}