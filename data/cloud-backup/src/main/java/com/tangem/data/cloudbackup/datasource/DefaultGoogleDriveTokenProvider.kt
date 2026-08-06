package com.tangem.data.cloudbackup.datasource

import android.content.Context
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.google.GoogleServicesHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Default [GoogleDriveTokenProvider]:
 * 1. bails out with [CloudBackupError.CloudUnavailable] when Google Play services are missing;
 * 2. serves the session-cached token without any user interaction;
 * 3. on a cache miss, delegates to [GoogleDriveAuthorizer] (honoring `interactive`) and caches the result.
 *
 * The 401/403 → re-auth path is handled by the repository mapping to [CloudBackupError.AuthPermissionsMissing];
 * this provider intentionally exposes nothing extra.
 */
internal class DefaultGoogleDriveTokenProvider(
    private val authorizer: GoogleDriveAuthorizer,
    private val api: GoogleDriveApi,
    private val context: Context,
) : GoogleDriveTokenProvider {

    private val authMutex = Mutex()

    private var cachedToken: String? = null

    override suspend fun getAccessToken(interactive: Boolean): Either<CloudBackupError, String> {
        if (!GoogleServicesHelper.checkGoogleServicesAvailability(context)) {
            return CloudBackupError.CloudUnavailable.left()
        }

        // serialize authorization so two concurrent cache misses don't open two account pickers
        return authMutex.withLock {
            cachedToken?.let { return@withLock it.right() }
            authorizer.authorize(interactive = interactive).map { result ->
                cachedToken = result.accessToken
                result.accessToken
            }
        }
    }

    override suspend fun invalidate() {
        authMutex.withLock {
            cachedToken?.let { authorizer.clearToken(it) }
            cachedToken = null
            authorizer.clearAuthorization()
        }
    }

    override suspend fun signOut() {
        authMutex.withLock {
            cachedToken?.let { token ->
                revokeQuietly(token)
                authorizer.clearToken(token)
            }
            cachedToken = null
            authorizer.clearAuthorization()
        }
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
}