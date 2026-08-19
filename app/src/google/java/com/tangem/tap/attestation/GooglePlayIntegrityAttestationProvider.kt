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
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    private val deviceKeyManager: DeviceKeyManager,
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
        val cloudProjectNumber = FirebaseApp.getInstance().options.gcmSenderId?.toLongOrNull()
            ?: run {
                TangemLogger.e("Play Integrity: cloud project number unavailable")
                return@withContext null
            }
        val devicePublicKey = deviceKeyManager.getPublicKeyEncoded().getOrNull()
            ?: return@withContext null

        val requestHash = AttestationRequestHash.create(devicePublicKey, nonce)
        val provider = obtainTokenProvider(cloudProjectNumber) ?: return@withContext null

        runSuspendCatching {
            provider
                .request(StandardIntegrityTokenRequest.builder().setRequestHash(requestHash).build())
                .await()
                .token()
        }.getOrElse { e ->
            // A prepared provider can expire; drop it so the next attempt re-prepares.
            TangemLogger.e("Play Integrity token request failed", e)
            tokenProvider = null
            null
        }
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
}