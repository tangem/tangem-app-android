package com.tangem.tap.domain.walletregistration

import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.lib.auth.session.WalletSignatureBundle
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.operations.attestation.AttestWalletKeyResponse
import com.tangem.operations.attestation.AttestWalletKeyTask
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Builds a [WalletSigner] for a COLD (card-backed) wallet. Runs `AttestWalletKeyTask` in Dynamic
 * mode either **inside an already-open [CardSession]** (no extra tap) or in a session of its own: the
 * card produces the wallet signature and, on COS 2.01+, the card signature over the wallet nonce. On
 * older cards without a card signature the wallet is registered with the wallet signature only
 * (backend treats it as hot).
 */
internal class ColdWalletRegistrationSigner @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    fun signerFor(session: CardSession, scanResponse: ScanResponse): WalletSigner =
        signerFor(session, scanResponse.card)

    /**
     * Signs in a card session of its own that accepts only a card of [userWallet], for a registration the user asked
     * for. No network call runs inside the session. A failure is thrown as the card SDK error, so a user
     * cancellation can be told from a failed signature.
     */
    fun signerFor(userWallet: UserWallet.Cold): WalletSigner = WalletSigner { nonceBytes ->
        val result = withContext(dispatchers.main) {
            suspendCancellableCoroutine { continuation ->
                cardSdkConfigRepository.sdk.startSessionWithRunnable(
                    runnable = SignNonceRunnable(nonceBytes),
                    preflightReadFilter = UserWalletIdPreflightReadFilter(userWallet),
                ) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            }
        }

        when (result) {
            is CompletionResult.Success -> result.data
            is CompletionResult.Failure -> throw result.error
        }
    }

    fun signerFor(session: CardSession, card: CardDTO): WalletSigner = WalletSigner { nonceBytes ->
        val walletPublicKey = firstSecpWalletPublicKey(card)

        // nonceBytes are already the raw challenge bytes (decoded once in DefaultWalletRegistrar) —
        // exactly what AttestWalletKeyTask needs as the challenge and what the backend verifies over.
        buildBundle(
            response = attest(session, walletPublicKey, nonceBytes),
            walletPublicKey = walletPublicKey,
            cardPublicKey = card.cardPublicKey,
            nonceBytes = nonceBytes,
        )
    }

    /**
     * Cold registration signs the **first** wallet on the card, which is expected to be secp256k1.
     * If it is not (an unexpected card layout), report it via analytics and abort — better than
     * silently registering a key on the wrong curve.
     */
    internal fun firstSecpWalletPublicKey(card: CardDTO): ByteArray {
        val wallet = card.wallets.firstOrNull()
            ?: error("No wallets on card ${card.cardId}")

        if (wallet.curve != EllipticCurve.Secp256k1) {
            analyticsEventHandler.send(BackendAuthAnalyticsEvent.UnexpectedWalletCurve(cardId = card.cardId))
            throw ColdWalletUnexpectedCurveException(card.cardId, wallet.curve)
        }

        return wallet.publicKey
            ?: error("First wallet has no public key on card ${card.cardId}")
    }

    /**
     * Pure mapping of an [AttestWalletKeyResponse] to a [WalletSignatureBundle] (no card session) —
     * unit-testable in isolation.
     */
    fun buildBundle(
        response: AttestWalletKeyResponse,
        walletPublicKey: ByteArray,
        cardPublicKey: ByteArray,
        nonceBytes: ByteArray,
    ): WalletSignatureBundle {
        val salt = response.salt
        val walletSignatureRsv = UnmarshalHelper.unmarshalSignatureExtended(
            signature = response.walletSignature,
            hash = (nonceBytes + salt).calculateSha256(),
            publicKey = walletPublicKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM()

        val cardSignature = response.cardSignature
        val publicKeySalt = response.publicKeySalt
        // The dynamic card signature (proving the card owns the wallet) requires COS 2.01+. On older
        // cards it is absent — we then register with the wallet signature only, and the backend
        // treats such a wallet as hot (no card-ownership proof).
        if (cardSignature == null || publicKeySalt == null) {
            return WalletSignatureBundle(
                walletSignature = walletSignatureRsv,
                walletSignatureSalt = salt,
                cardSignature = null,
                cardSignatureSalt = null,
                walletStatusByte = null,
            )
        }

        val walletStatusByte = response.walletStatus?.code?.toByte()
        // Card-signature preimage: walletPublicKey | challenge | publicKeySalt [| walletStatus].
        // The walletStatus byte is appended only when the card reports it (COS 6+).
        val cardMessage = walletPublicKey + nonceBytes + publicKeySalt +
            (walletStatusByte?.let { byteArrayOf(it) } ?: ByteArray(size = 0))
        val cardSignatureRsv = UnmarshalHelper.unmarshalSignatureExtended(
            signature = cardSignature,
            hash = cardMessage.calculateSha256(),
            publicKey = cardPublicKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM()

        return WalletSignatureBundle(
            walletSignature = walletSignatureRsv,
            walletSignatureSalt = salt,
            cardSignature = cardSignatureRsv,
            cardSignatureSalt = publicKeySalt,
            walletStatusByte = walletStatusByte,
        )
    }

    private suspend fun attest(
        session: CardSession,
        walletPublicKey: ByteArray,
        nonceBytes: ByteArray,
    ): AttestWalletKeyResponse {
        val result = suspendCancellableCoroutine { continuation ->
            AttestWalletKeyTask(publicKey = walletPublicKey, challenge = nonceBytes)
                .run(session) { if (continuation.isActive) continuation.resume(it) }
        }
        return when (result) {
            is CompletionResult.Success -> result.data
            is CompletionResult.Failure -> throw ColdWalletAttestationException(result.error)
        }
    }

    private inner class SignNonceRunnable(
        private val nonceBytes: ByteArray,
    ) : CardSessionRunnable<WalletSignatureBundle> {

        override fun run(session: CardSession, callback: (result: CompletionResult<WalletSignatureBundle>) -> Unit) {
            val card = session.environment.card?.let(::CardDTO)
                ?: return callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))

            session.scope.launch {
                val result = runSuspendCatching { signerFor(session, card).sign(nonceBytes) }.fold(
                    onSuccess = { bundle -> CompletionResult.Success(bundle) },
                    onFailure = { error -> CompletionResult.Failure(error.toTangemError()) },
                )
                callback(result)
            }
        }

        private fun Throwable.toTangemError(): TangemError = when (this) {
            is ColdWalletAttestationException -> error
            is TangemError -> this
            else -> TangemSdkError.ExceptionError(this)
        }
    }
}

/** `AttestWalletKeyTask` failed (NFC error, verification failure, user cancelled). */
internal class ColdWalletAttestationException(val error: TangemError) :
    Exception("Cold wallet attestation failed: ${error.customMessage}", error)

/** The first wallet on the card is not secp256k1 — unexpected for cold registration; abort. */
internal class ColdWalletUnexpectedCurveException(cardId: String, curve: EllipticCurve) :
    Exception("Cold wallet registration aborted: first wallet curve is $curve, expected Secp256k1 (card $cardId)")