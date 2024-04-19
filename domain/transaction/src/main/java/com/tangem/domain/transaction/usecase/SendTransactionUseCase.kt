package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.network.ResultChecker
import com.tangem.common.core.TangemSdkError
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.R
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SendTransactionError.Companion.USER_CANCELLED_ERROR_CODE
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.utils.toFormattedString

class SendTransactionUseCase(
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val transactionRepository: TransactionRepository,
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
                transactionRepository.sendTransaction(
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
        if (ResultChecker.isNetworkError(result)) {
            return SendTransactionError.NetworkError(
                code = result.error.message,
                message = result.error.customMessage,
            )
        }
        val error = result.error as? BlockchainSdkError ?: return SendTransactionError.UnknownError()
        return when (error) {
            is BlockchainSdkError.WrappedTangemError -> {
                if (error.code == USER_CANCELLED_ERROR_CODE) {
                    SendTransactionError.UserCancelledError
                } else {
                    val tangemError = error.tangemError
                    if (tangemError is TangemSdkError) {
                        val resource = tangemError.localizedDescriptionRes()
                        val resId = resource.resId ?: R.string.common_unknown_error
                        val resArgs = resource.args.map { it.value }
                        val textReference = resourceReference(resId, wrappedList(resArgs))
                        SendTransactionError.TangemSdkError(tangemError.code, textReference)
                    } else {
                        SendTransactionError.BlockchainSdkError(error.code, tangemError.customMessage)
                    }
                }
            }
            is BlockchainSdkError.CreateAccountUnderfunded -> {
                val minAmount = error.minReserve
                val minValue = minAmount.value?.toFormattedString(minAmount.decimals).orEmpty()
                SendTransactionError.CreateAccountUnderfunded(minValue)
            }
            else -> {
                SendTransactionError.BlockchainSdkError(
                    code = error.code,
                    message = error.customMessage,
                )
            }
        }
    }
}
