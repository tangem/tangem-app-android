package com.tangem.google.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [GoogleAuthorizer] backed by the Google Identity Services `AuthorizationClient`.
 *
 * Requests exactly the caller-provided scopes, so a feature grants only what it needs. When the
 * client needs user interaction it returns a resolution `PendingIntent`, which is launched through
 * [GoogleAuthActivityResultBridge]. The access token is never logged.
 */
@Singleton
internal class GoogleIdentityAuthorizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityResultBridge: GoogleAuthActivityResultBridge,
    private val dispatchers: CoroutineDispatcherProvider,
) : GoogleAuthorizer {

    private val logger = TangemLogger.withTag(TAG)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun authorize(
        scopes: List<GoogleAuthScope>,
        interactive: Boolean,
    ): Either<GoogleAuthError, GoogleAuthResult> {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(scopes.map { Scope(it.value) })
                .build()

            val result = Identity.getAuthorizationClient(context)
                .authorize(request)
                .await()

            resolveResult(result, interactive)
        } catch (e: ApiException) {
            logger.e("Google authorization failed: statusCode=${e.statusCode}")
            mapApiException(e).left()
        } catch (e: IOException) {
            logger.e("Google authorization network error")
            GoogleAuthError.NetworkError.left()
        } catch (e: GoogleAuthLauncherUnavailableException) {
            logger.e("Google authorization consent UI is unavailable")
            GoogleAuthError.Unknown(e).left()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e("Google authorization unexpected error")
            GoogleAuthError.Unknown(e).left()
        }
    }

    override fun clearAuthorization() {
        // Nothing is cached here: AuthorizationClient is created per call and exposes no revocation,
        // so forgetting the account means the caller dropping its token (see clearToken)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override suspend fun clearToken(token: String) {
        withContext(dispatchers.io) {
            try {
                GoogleAuthUtil.clearToken(context, token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e("Google token cache clear failed")
            }
        }
    }

    private suspend fun resolveResult(
        result: AuthorizationResult,
        interactive: Boolean,
    ): Either<GoogleAuthError, GoogleAuthResult> {
        if (!result.hasResolution()) {
            return toAuthResult(result.accessToken)
        }

        if (!interactive) {
            return GoogleAuthError.AuthRequired.left()
        }

        val intentSender = result.pendingIntent?.intentSender
            ?: return GoogleAuthError.Unknown().left()

        val activityResult = activityResultBridge.launch(intentSender)
        return when (activityResult.resultCode) {
            Activity.RESULT_OK -> resolveFromIntent(activityResult.data)
            Activity.RESULT_CANCELED -> GoogleAuthError.AuthCanceled.left()
            else -> GoogleAuthError.Unknown().left()
        }
    }

    private fun resolveFromIntent(data: Intent?): Either<GoogleAuthError, GoogleAuthResult> {
        val intent = data ?: return GoogleAuthError.AuthCanceled.left()
        return try {
            val result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(intent)
            toAuthResult(result.accessToken)
        } catch (e: ApiException) {
            logger.e("Google authorization resolution failed: statusCode=${e.statusCode}")
            mapApiException(e).left()
        }
    }

    private fun toAuthResult(accessToken: String?): Either<GoogleAuthError, GoogleAuthResult> {
        val token = accessToken ?: return GoogleAuthError.PermissionsMissing.left()
        return GoogleAuthResult(accessToken = token).right()
    }

    private fun mapApiException(e: ApiException): GoogleAuthError =
        googleAuthErrorForStatusCode(e.statusCode) ?: GoogleAuthError.Unknown(e)

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val TAG = "GoogleIdentityAuthorizer"
    }
}

/**
 * Maps a Google Identity [CommonStatusCodes] status code to a [GoogleAuthError], or `null` when the
 * code is unrecognized (the caller wraps it as [GoogleAuthError.Unknown] with the original cause).
 */
internal fun googleAuthErrorForStatusCode(statusCode: Int): GoogleAuthError? = when (statusCode) {
    CommonStatusCodes.NETWORK_ERROR,
    CommonStatusCodes.TIMEOUT,
    -> GoogleAuthError.NetworkError
    CommonStatusCodes.SIGN_IN_REQUIRED,
    CommonStatusCodes.RESOLUTION_REQUIRED,
    -> GoogleAuthError.AuthRequired
    CommonStatusCodes.INVALID_ACCOUNT -> GoogleAuthError.PermissionsMissing
    CommonStatusCodes.API_NOT_CONNECTED,
    CommonStatusCodes.SERVICE_DISABLED,
    CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED,
    -> GoogleAuthError.Unavailable
    CommonStatusCodes.CANCELED -> GoogleAuthError.AuthCanceled
    else -> null
}