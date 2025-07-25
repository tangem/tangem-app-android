package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.requireColdWallet

class PrepareForSendUseCase(
    private val transactionRepository: TransactionRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
) {
    suspend operator fun invoke(
        transactionData: TransactionData,
        userWallet: UserWallet,
        network: Network,
    ): Either<SendTransactionError, ByteArray> {
        val signer = createSigner(userWallet)
        val result = transactionRepository.prepareForSend(
            transactionData = transactionData,
            userWalletId = userWallet.walletId,
            network = network,
            signer = signer,
        )
        return when (result) {
            is Result.Failure -> SendTransactionUseCase.handleError(result).left()
            is Result.Success -> result.data.right()
        }
    }

    suspend operator fun invoke(
        transactionData: List<TransactionData>,
        userWallet: UserWallet,
        network: Network,
    ): Either<SendTransactionError, List<ByteArray>> {
        val signer = createSigner(userWallet)
        val result = transactionRepository.prepareForSendMultiple(
            transactionData = transactionData,
            userWalletId = userWallet.walletId,
            network = network,
            signer = signer,
        )
        return when (result) {
            is Result.Failure -> SendTransactionUseCase.handleError(result).left()
            is Result.Success -> result.data.right()
        }
    }

    private fun createSigner(userWallet: UserWallet): TransactionSigner {
        userWallet.requireColdWallet() // TODO [REDACTED_TASK_KEY]
        val card = userWallet.scanResponse.card
        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

        val signer = cardSdkConfigRepository.getCommonSigner(
            cardId = card.cardId.takeIf { isCardNotBackedUp },
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
        )
        return signer
    }
}