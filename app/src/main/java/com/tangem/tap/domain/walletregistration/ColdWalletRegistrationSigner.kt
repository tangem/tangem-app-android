package com.tangem.tap.domain.walletregistration

import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toDecompressedPublicKey
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
 * mode **inside an already-open [CardSession]** (no extra tap): the card produces the wallet
 * signature and, on COS 2.01+, the card signature over the wallet nonce. On older cards without a
 * card signature the wallet is registered with the wallet signature only (backend treats it as hot).
 */
internal class ColdWalletRegistrationSigner @Inject constructor() {

    fun signerFor(session: CardSession, scanResponse: ScanResponse): WalletSigner = WalletSigner { nonceBytes ->
        val card = scanResponse.card
        val walletPublicKey = card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 }?.publicKey
            ?: error("No secp256k1 wallet on card ${card.cardId}")

        buildBundle(
            response = attest(session, walletPublicKey, nonceBytes),
            walletPublicKey = walletPublicKey,
            cardPublicKey = card.cardPublicKey,
            nonceBytes = nonceBytes,
        )
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
            is CompletionResult.Failure -> throw ColdWalletAttestationException(result.error.customMessage)
        }
    }
}

/** `AttestWalletKeyTask` failed (NFC error, verification failure, user cancelled). */
internal class ColdWalletAttestationException(message: String) :
    Exception("Cold wallet attestation failed: $message")