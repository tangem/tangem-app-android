package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.VerifyMessagesError

/**
 * Verifies each of the given [messages] against [userWallet]'s primary secp256k1 key — the
 * counterpart of [SignUseCase] for raw-hash signatures.
 *
 * Only secp256k1 is supported: the verifier hashes the message with SHA-256 and runs ECDSA, which
 * mirrors how the card signs (`SHA-256(message)` then ECDSA). Other curves use a different hashing
 * scheme, so this use case is deliberately curve-specific rather than parameterized.
 *
 * Pass the **original messages** (the pre-images), not their hashes: the SHA-256 is applied
 * internally (via [CryptoUtils.verify]). [messages] and [signatures] are positional — element `i` of
 * one must correspond to element `i` of the other.
 *
 * Returns one [Boolean] per message, aligned to [messages] order: `result[i]` is `true` only when
 * `signatures[i]` is a valid signature of `messages[i]`. A mismatch (tampered data, wrong wallet,
 * malformed signature) or a missing signature for that index yields `false` for that element. The
 * wallet's signing key being unavailable is a [VerifyMessagesError.NoSigningKey] failure (nothing can
 * be verified) rather than a list of `false`s.
 */
class VerifySecp256k1MessagesUseCase {

    operator fun invoke(
        userWallet: UserWallet,
        messages: List<ByteArray>,
        signatures: List<ByteArray>,
    ): Either<VerifyMessagesError, List<Boolean>> {
        val publicKey = userWallet.primarySecp256k1PublicKey()
            ?: return VerifyMessagesError.NoSigningKey.left()

        val results = messages.mapIndexed { index, message ->
            val signature = signatures.getOrNull(index) ?: return@mapIndexed false
            CryptoUtils.verify(
                publicKey = publicKey,
                message = message,
                signature = signature,
                curve = EllipticCurve.Secp256k1,
            )
        }
        return results.right()
    }
}