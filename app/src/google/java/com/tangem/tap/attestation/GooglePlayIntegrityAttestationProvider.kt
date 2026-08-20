package com.tangem.tap.attestation

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.firebase.FirebaseApp
import com.tangem.google.GoogleServicesHelper
import com.tangem.lib.auth.attestation.AttestationProvider
import com.tangem.lib.auth.attestation.AttestationRequestHash
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Google Play Integrity (Standard) implementation of [AttestationProvider].
 *
 * Warms up a [StandardIntegrityTokenProvider] once (cached), then requests a token bound by
 * `requestHash = Base64(SHA-256(devicePublicKey ‖ nonce))` per [AttestationRequestHash]. Strictly
 * best-effort: unavailable Google Play services, a missing device key / cloud project number, or any
 * platform failure resolves to `null` and never blocks authentication.
 */
internal class GooglePlayIntegrityAttestationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : AttestationProvider {

    private val manager: StandardIntegrityManager by lazy { IntegrityManagerFactory.createStandard(context) }
    private val prepareMutex = Mutex()

    @Volatile
    private var tokenProvider: StandardIntegrityTokenProvider? = null

    override suspend fun getAttestationToken(nonce: String): String? = withContext(dispatchers.io) {
        if (!GoogleServicesHelper.checkGoogleServicesAvailability(context)) {
            TangemLogger.i("Play Integrity unavailable — Google Play services missing")
            return@withContext null
        }
        val cloudProjectNumber = runCatching { FirebaseApp.getInstance().options.gcmSenderId?.toLongOrNull() }
            .getOrNull()
            ?: run {
                TangemLogger.e("Play Integrity: cloud project number unavailable — returning null token")
                return@withContext null
            }
        val requestHash = AttestationRequestHash.create(nonce)

        // Attestation is inline before signing, so a stalled Play Integrity call would block auth
        // (including /authenticate on 401 refresh). Cap the whole warm-up + request so a hang or slow
        // network resolves to a null token and auth proceeds.
        val token = withTimeoutOrNull(ATTESTATION_TIMEOUT_MS) {
            val provider = obtainTokenProvider(cloudProjectNumber)
                ?: return@withTimeoutOrNull null // prepare failure already logged in obtainTokenProvider
            runSuspendCatching {
                provider
                    .request(StandardIntegrityTokenRequest.builder().setRequestHash(requestHash).build())
                    .await()
                    .token()
            }.getOrElse { e ->
                // A prepared provider can expire; drop it so the next attempt re-prepares.
                TangemLogger.e("Play Integrity token request failed — returning null token", e)
                tokenProvider = null
                null
            }
        }
        if (token == null) {
            TangemLogger.i("Play Integrity produced no token (timeout or failure) — returning null")
        }
        token
    }

    private suspend fun obtainTokenProvider(cloudProjectNumber: Long): StandardIntegrityTokenProvider? {
        tokenProvider?.let { return it }
        return prepareMutex.withLock {
            tokenProvider ?: runSuspendCatching {
                manager
                    .prepareIntegrityToken(
                        PrepareIntegrityTokenRequest.builder()
                            .setCloudProjectNumber(cloudProjectNumber)
                            .build(),
                    )
                    .await()
            }.onFailure { TangemLogger.e("Play Integrity prepare failed", it) }
                .getOrNull()
                ?.also { tokenProvider = it }
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { e -> continuation.resumeWithException(e) }
    }

    private companion object {
        // Generous enough for a first-call warm-up, bounded so attestation can never block auth.
        const val ATTESTATION_TIMEOUT_MS = 10_000L
    }
}