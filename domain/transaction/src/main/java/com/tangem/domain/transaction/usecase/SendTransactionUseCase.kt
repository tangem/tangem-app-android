package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.ResultChecker
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SendTransactionError.Companion.USER_CANCELLED_ERROR_CODE
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
        val signer = cardSdkConfigRepository.getCommonSigner(cardId = null)

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
                    is SimpleResult.Failure -> handleError(result).left()
                }
            },
            ifLeft = { it.left() },
        )
    }

    private fun handleError(result: SimpleResult.Failure): SendTransactionError {
        if (ResultChecker.isNetworkError(result)) return SendTransactionError.NetworkError(result.error.message)
        val error = result.error as? BlockchainSdkError ?: return SendTransactionError.UnknownError()
        when (error) {
            is BlockchainSdkError.WrappedTangemError -> {
                val errorByCode = mapErrorByCode(error)
                if (errorByCode != null) {
                    return errorByCode
                }
                val tangemSdkError = error.tangemError as? TangemSdkError ?: return SendTransactionError.UnknownError()
                if (tangemSdkError is TangemSdkError.UserCancelled) return SendTransactionError.UserCancelledError
                return SendTransactionError.TangemSdkError(tangemSdkError.code, tangemSdkError.cause)
            }
            else -> {
                return SendTransactionError.TangemSdkError(error.code, error.cause)
            }
        }
    }

    private fun mapErrorByCode(error: BlockchainSdkError.WrappedTangemError): SendTransactionError? {
        return when (error.code) {
            USER_CANCELLED_ERROR_CODE -> {
                return SendTransactionError.UserCancelledError
            }
            else -> {
                null
            }
        }
    }
}