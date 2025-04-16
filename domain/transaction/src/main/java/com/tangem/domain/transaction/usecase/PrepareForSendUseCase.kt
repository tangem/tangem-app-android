package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.wallets.models.UserWallet

class PrepareForSendUseCase(
    private val transactionRepository: TransactionRepository,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
) {
    suspend operator fun invoke(
        transactionData: TransactionData,
        userWallet: UserWallet,
        network: Network,
    ): Either<Throwable, ByteArray> {
        val card = userWallet.scanResponse.card
        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

        val signer = cardSdkConfigRepository.getCommonSigner(
            cardId = card.cardId.takeIf { isCardNotBackedUp },
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
        )
        return transactionRepository.prepareForSend(
            transactionData = transactionData,
            userWalletId = userWallet.walletId,
            network = network,
            signer = signer,
        )
            .fold(onSuccess = { it.right() }, onFailure = { it.left() })
    }
}
