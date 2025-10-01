package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.blockchains.solana.SolanaWalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.walletmanager.WalletManagersFacade

/**
 * Use case for sending large Solana transactions that cannot be signed directly by the card.
 * It handles the transaction creating alt tables and sending the transaction through the Solana network.
 *
 * @property cardSdkConfigRepository Repository to access card SDK configurations and signers.
 * @property walletManagersFacade Facade to manage and retrieve wallet managers.
 */
class SendLargeSolanaTransactionUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {

    suspend operator fun invoke(
        userWallet: UserWallet.Cold,
        network: Network,
        txHash: ByteArray,
    ): Either<SendTransactionError, Unit> {
        val card = userWallet.scanResponse.card
        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

        val signer = cardSdkConfigRepository.getCommonSigner(
            cardId = card.cardId.takeIf { isCardNotBackedUp },
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
        )

        val walletManager = walletManagersFacade
            .getOrCreateWalletManager(userWallet.walletId, network)
            ?: error("WalletManager is null")

        if (walletManager !is SolanaWalletManager) return SendTransactionError.UnknownError().left()
        val result = walletManager.handleLargeLegacyTransaction(signer, txHash)
        return when (result) {
            is Result.Failure -> SendTransactionError.BlockchainSdkError(
                code = result.error.code,
                message = result.error.customMessage,
            ).left()
            is Result.Success -> Unit.right()
        }
    }
}