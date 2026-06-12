package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SignHashesError

/**
 * Signs a batch of raw [hashes] with the wallet's primary secp256k1 key in a single signing
 * session — one NFC tap for cold cards, one access-code unlock for hot wallets.
 *
 * The hashes are signed with the wallet master key without any network derivation, so every
 * signature verifies against that single wallet public key regardless of which networks the hashed
 * data refers to. Use it when several pieces of data must be attested with the same wallet identity
 * in one user interaction (e.g. signing all address-book entries of a contact at once).
 *
 * Signatures are returned in the same order as the input [hashes].
 */
class SignHashesUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val getHotTransactionSigner: (UserWallet.Hot) -> TransactionSigner,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        hashes: List<ByteArray>,
    ): Either<SignHashesError, List<ByteArray>> {
        if (hashes.isEmpty()) return emptyList<ByteArray>().right()

        val seedKey = userWallet.primarySecp256k1PublicKey() ?: return SignHashesError.NoSigningKey.left()
        val publicKey = Wallet.PublicKey(seedKey = seedKey, derivationType = null)

        val signer = when (userWallet) {
            is UserWallet.Hot -> getHotTransactionSigner(userWallet)
            is UserWallet.Cold -> getColdSigner(userWallet)
        }

        return when (val result = signer.sign(hashes, publicKey)) {
            is CompletionResult.Success -> result.data.right()
            is CompletionResult.Failure -> SignHashesError.SigningFailed(
                message = result.error.message ?: "Unknown error",
            ).left()
        }
    }

    private fun getColdSigner(userWallet: UserWallet.Cold): TransactionSigner {
        val card = userWallet.scanResponse.card
        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

        return cardSdkConfigRepository.getCommonSigner(
            cardId = card.cardId.takeIf { isCardNotBackedUp },
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
        )
    }
}