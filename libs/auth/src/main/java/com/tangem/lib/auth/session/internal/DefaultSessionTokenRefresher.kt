package com.tangem.lib.auth.session.internal

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.datasource.api.auth.AuthApi
import com.tangem.datasource.api.auth.models.request.AuthApiRequest
import com.tangem.datasource.api.auth.models.request.AuthenticationPayload
import com.tangem.datasource.api.auth.models.request.NonceApiRequest
import com.tangem.datasource.api.auth.models.request.RefreshApiRequest
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.SessionRefreshError
import com.tangem.lib.auth.session.SessionTokenRefresher
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

@Suppress("LongParameterList")
internal class DefaultSessionTokenRefresher(
    private val authApi: AuthApi,
    private val store: SessionTokensStore,
    private val deviceKeyManager: DeviceKeyManager,
    private val nonceDecryptor: AuthNonceDecryptor,
    private val signedRequestPayload: SignedRequestPayload,
    private val errorConverter: AuthErrorConverter,
    private val clock: Clock,
    private val dispatchers: CoroutineDispatcherProvider,
) : SessionTokenRefresher {

    private val mutex = Mutex()
    private var inFlight: CompletableDeferred<Either<SessionRefreshError, SessionTokens>>? = null

    override suspend fun refresh(): Either<SessionRefreshError, SessionTokens> = withContext(dispatchers.io) {
        // True single-flight (SR-8 / RFC 9449 §5): concurrent callers share one network round-trip
        // and receive the same result — both on success and on transient failures. This prevents
        // refresh-token replay (which revokes the family) and avoids amplifying outages/rate limits.
        var isOwner = false
        val deferred = mutex.withLock {
            inFlight ?: CompletableDeferred<Either<SessionRefreshError, SessionTokens>>().also { deferred ->
                inFlight = deferred
                isOwner = true
            }
        }

        if (isOwner) {
            try {
                deferred.complete(runRefresh(current = store.get().getOrNull()))
            } catch (t: Throwable) {
                // Propagate to every waiter — without this they'd suspend forever on `await()`.
                deferred.completeExceptionally(t)
                throw t
            } finally {
                // `NonCancellable` keeps the slot-clearing alive even if the owner coroutine is
                // cancelled mid-refresh, so the next caller can start a fresh attempt.
                withContext(NonCancellable) {
                    mutex.withLock { inFlight = null }
                }
            }
        }

        deferred.await()
    }

    private suspend fun runRefresh(current: SessionTokens?): Either<SessionRefreshError, SessionTokens> {
        val now = clock.now()

        val isRefreshTokenValid = current?.refreshTokenExpiresAt != null && current.refreshTokenExpiresAt > now
        if (current?.refreshToken != null && isRefreshTokenValid) {
            when (val result = callRefresh(current.refreshToken)) {
                is RefreshOutcome.Success -> return result.tokens.right()
                RefreshOutcome.Unauthenticated -> Unit // fall through to /authenticate
                is RefreshOutcome.Transient -> return SessionRefreshError.Api(result.cause).left()
            }
        }

        return runAuthenticate()
    }

    private suspend fun callRefresh(refreshToken: String): RefreshOutcome {
        val response = authApi.refresh(RefreshApiRequest(refreshToken = refreshToken))
        return handleTokenResponse(response, clearOnUnauthenticated = false)
    }

    private suspend fun runAuthenticate(): Either<SessionRefreshError, SessionTokens> = either {
        val devicePublicKey = deviceKeyManager.getPublicKey().getOrNull()
            ?: raise(SessionRefreshError.DeviceKeyUnavailable)

        val devicePublicKeyBase64 = devicePublicKey.toBase64NoWrap()

        val nonceResponse = authApi.requestAuthNonce(NonceApiRequest(devicePublicKey = devicePublicKeyBase64))
        val cipheredNonce = when (nonceResponse) {
            is ApiResponse.Success -> nonceResponse.data.cipheredNonce
            is ApiResponse.Error -> {
                val authError = errorConverter.convert(nonceResponse.cause)
                raise(SessionRefreshError.Api(authError))
            }
        }

        val nonce = try {
            nonceDecryptor.decryptNonce(cipheredNonce)
        } catch (e: Exception) {
            TangemLogger.e("Failed to decrypt auth nonce", e)
            raise(SessionRefreshError.NonceDecryptionFailed(e))
        }

        val payload = AuthenticationPayload(
            devicePublicKey = devicePublicKeyBase64,
            nonce = nonce,
            attestationToken = null,
            metadata = signedRequestPayload.deviceMetadata,
        )
        val signature = try {
            deviceKeyManager.sign(signedRequestPayload.canonicalize(payload)).toBase64NoWrap()
        } catch (e: Exception) {
            TangemLogger.e("Failed to sign authentication payload", e)
            raise(SessionRefreshError.SigningFailed(e))
        }

        val authResponse = authApi.authenticate(AuthApiRequest(payload = payload, signature = signature))
        return when (val outcome = handleTokenResponse(authResponse, clearOnUnauthenticated = true)) {
            is RefreshOutcome.Success -> outcome.tokens.right()
            RefreshOutcome.Unauthenticated -> SessionRefreshError.SessionRevoked.left()
            is RefreshOutcome.Transient -> SessionRefreshError.Api(outcome.cause).left()
        }
    }

    private suspend fun handleTokenResponse(
        response: ApiResponse<TokenApiResponse>,
        clearOnUnauthenticated: Boolean,
    ): RefreshOutcome {
        return when (response) {
            is ApiResponse.Success -> {
                val tokens = SessionTokensConverter.convertBack(response.data)
                store.save(tokens)
                RefreshOutcome.Success(tokens)
            }
            is ApiResponse.Error -> {
                when (val authError = errorConverter.convert(response.cause)) {
                    is AuthError.Unauthorized, is AuthError.Forbidden -> {
                        if (clearOnUnauthenticated) {
                            TangemLogger.i("Session revoked: ${authError.problem?.detail ?: authError}")
                            store.clear()
                        }
                        RefreshOutcome.Unauthenticated
                    }
                    else -> RefreshOutcome.Transient(authError)
                }
            }
        }
    }

    private sealed interface RefreshOutcome {
        data class Success(val tokens: SessionTokens) : RefreshOutcome
        data object Unauthenticated : RefreshOutcome
        data class Transient(val cause: AuthError) : RefreshOutcome
    }
}