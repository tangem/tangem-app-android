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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

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
    private val api: GoogleDriveApi,
    private val context: Context,
    private val networkConnectionManager: NetworkConnectionManager,
    private val scopes: List<GoogleAuthScope> = listOf(DRIVE_FILE_SCOPE),
) : GoogleDriveTokenProvider {

    private val authMutex = Mutex()

    private var cachedToken: String? = null

    override suspend fun getAccessToken(interactive: Boolean): Either<GoogleAuthError, String> {
        if (!GoogleServicesHelper.checkGoogleServicesAvailability(context)) {
            return GoogleAuthError.Unavailable.left()
        }

        // serialize authorization so two concurrent cache misses don't open two account pickers
        return authMutex.withLock {
            cachedToken?.let { return@withLock it.right() }
            if (!networkConnectionManager.isOnline) return@withLock GoogleAuthError.NetworkError.left()

            authorizer.authorize(scopes = scopes, interactive = interactive).map { result ->
                cachedToken = result.accessToken
                result.accessToken
            }
        }
    }

    override suspend fun invalidate() {
        authMutex.withLock { dropAuthorization() }
    }

    override suspend fun switchAccount(accountName: String) {
        authMutex.withLock {
            dropAuthorization()
            authorizer.selectAccount(accountName)
        }
    }

    override suspend fun signOut() {
        authMutex.withLock {
            cachedToken?.let { revokeQuietly(it) }
            dropAuthorization()
        }
    }

    private suspend fun dropAuthorization() {
        cachedToken?.let { authorizer.clearToken(it) }
        cachedToken = null
        authorizer.clearAuthorization()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun revokeQuietly(token: String) {
        try {
            api.revokeToken(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // best-effort: local sign-out proceeds even if the remote revoke fails
        }
    }

    companion object {
        val DRIVE_FILE_SCOPE = GoogleAuthScope("https://www.googleapis.com/auth/drive.file")

        /** Grants access to the hidden `appDataFolder` only — nothing the user can see in their Drive */
        val DRIVE_APP_DATA_SCOPE = GoogleAuthScope("https://www.googleapis.com/auth/drive.appdata")
    }
}