package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet

class SendTransactionUseCase(
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {
    suspend operator fun invoke(
        txData: TransactionData,
        userWallet: UserWallet,
        network: Network,
    ): Either<SendTransactionError?, Boolean> {
        val signer = cardSdkConfigRepository.getCommonSigner(
            userWallet.cardId,
        )

        val linkedTerminal = cardSdkConfigRepository.isLinkedTerminal()
        if (userWallet.scanResponse.card.isStart2Coin) {
            cardSdkConfigRepository.setLinkedTerminal(false)
        }
        val sendResult = try {
            if (isDemoCardUseCase(cardId = userWallet.cardId)) {
                SendTransactionError.DemoCardError.left()
            } else {
                walletManagersFacade.sendTransaction(
                    txData = txData,
                    signer = signer,
                    userWalletId = userWallet.walletId,
                    network = network,
                ).right()
            }
        } catch (ex: Exception) {
            cardSdkConfigRepository.setLinkedTerminal(linkedTerminal)
            SendTransactionError.DataError(ex.message).left()
        }

        cardSdkConfigRepository.setLinkedTerminal(linkedTerminal)
        return sendResult.fold(
            ifRight = { result ->
                when (result) {
                    is SimpleResult.Success -> true.right()
                    is SimpleResult.Failure -> SendTransactionError.NetworkError(result.error.message).left()
                }
            },
            ifLeft = { it.left() },
        )
    }
}
