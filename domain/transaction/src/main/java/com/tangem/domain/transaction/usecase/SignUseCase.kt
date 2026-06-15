package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWallet

class SignUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val getHotTransactionSigner: (UserWallet.Hot) -> TransactionSigner,
) {
    suspend operator fun invoke(
        hash: ByteArray,
        userWallet: UserWallet,
        network: Network,
    ): Either<TangemError, ByteArray> {
        val signer = when (userWallet) {
            is UserWallet.Hot -> getHotTransactionSigner(userWallet)
            is UserWallet.Cold -> getColdSigner(userWallet)
        }

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
            ?: error("WalletManager not found")

        return when (val signResult = signer.sign(hash, walletManager.wallet.publicKey)) {
            is CompletionResult.Failure -> signResult.error.left()
            is CompletionResult.Success -> signResult.data.right()
        }
    }

    /**
     * Signs a batch of raw [hashes] with [publicKey] in a single signing session — one NFC tap for
     * cold cards, one access-code unlock for hot wallets.
     *
     * [publicKey] is the wallet seed key the hashes are signed with, without any network derivation,
     * so every signature verifies against that single public key regardless of which networks the
     * hashed data refers to. Resolving which key to sign with (and its curve) is the caller's
     * responsibility — this use case is curve-agnostic and signs exactly the given hashes.
     *
     * Signatures are returned in the same order as the input [hashes]; an empty input yields an empty
     * list without starting a signing session.
     */
    suspend operator fun invoke(
        hashes: List<ByteArray>,
        publicKey: ByteArray,
        userWallet: UserWallet,
    ): Either<SignHashesError, List<ByteArray>> {
        if (hashes.isEmpty()) return emptyList<ByteArray>().right()

        val signer = when (userWallet) {
            is UserWallet.Hot -> getHotTransactionSigner(userWallet)
            is UserWallet.Cold -> getColdSigner(userWallet)
        }
        val seedPublicKey = Wallet.PublicKey(seedKey = publicKey, derivationType = null)

        return when (val result = signer.sign(hashes, seedPublicKey)) {
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