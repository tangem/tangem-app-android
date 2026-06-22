package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.walletmanager.WalletManagersFacade

/**
 * Signs and broadcasts a Bitcoin swap transaction supplied by a DEX provider as a Base64 PSBT.
 *
 * Unlike a normal send (where we build the transaction ourselves), the provider returns an
 * almost-complete transaction encoded as a PSBT in `txData`. This use case:
 *  1. derives which inputs belong to the wallet ([WalletManager.deriveSignInputs]),
 *  2. signs them with the card/hot signer ([WalletManager.signPsbt]),
 *  3. finalizes and broadcasts the transaction ([WalletManager.broadcastPsbt]),
 *
 * returning the resulting transaction hash. Errors from any step are mapped to [SendTransactionError]
 * via [SendTransactionUseCase.handleError].
 */
class SignAndBroadcastPsbtUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val getHotTransactionSigner: (UserWallet.Hot) -> TransactionSigner,
) {

    suspend operator fun invoke(
        psbtBase64: String,
        userWallet: UserWallet,
        network: Network,
    ): Either<SendTransactionError, String> {
        walletManagersFacade.update(
            userWalletId = userWallet.walletId,
            network = network,
            extraTokens = emptySet(),
        )

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
            ?: return SendTransactionError.UnknownError().left()

        val signInputs = when (val result = walletManager.deriveSignInputs(psbtBase64)) {
            is Result.Success -> result.data
            is Result.Failure -> return SendTransactionUseCase.handleError(result).left()
        }

        val signer = createSigner(userWallet)

        val signedPsbt = when (val result = walletManager.signPsbt(psbtBase64, signInputs, signer)) {
            is Result.Success -> result.data
            is Result.Failure -> return SendTransactionUseCase.handleError(result).left()
        }

        return when (val result = walletManager.broadcastPsbt(signedPsbt)) {
            is Result.Success -> result.data.right()
            is Result.Failure -> SendTransactionUseCase.handleError(result).left()
        }
    }

    private fun createSigner(userWallet: UserWallet): TransactionSigner {
        return when (userWallet) {
            is UserWallet.Hot -> getHotTransactionSigner(userWallet)
            is UserWallet.Cold -> {
                val card = userWallet.scanResponse.card
                val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins
                cardSdkConfigRepository.getCommonSigner(
                    cardId = card.cardId.takeIf { isCardNotBackedUp },
                    twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
                    userWalletId = userWallet.walletId,
                )
            }
        }
    }
}