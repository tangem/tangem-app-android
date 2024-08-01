package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.ResultChecker
import com.tangem.common.core.TangemSdkError
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.R
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SendTransactionError.Companion.USER_CANCELLED_ERROR_CODE
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.utils.toFormattedString

class SendTransactionUseCase(
    private val demoConfig: DemoConfig,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val transactionRepository: TransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {
    suspend operator fun invoke(
        txData: TransactionData,
        userWallet: UserWallet,
        network: Network,
    ): Either<SendTransactionError?, String> {
        val card = userWallet.scanResponse.card
        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

        val signer = cardSdkConfigRepository.getCommonSigner(cardId = card.cardId.takeIf { isCardNotBackedUp })

        val linkedTerminal = cardSdkConfigRepository.isLinkedTerminal()
        if (userWallet.scanResponse.card.isStart2Coin) {
            cardSdkConfigRepository.setLinkedTerminal(false)
        }
        val sendResult = try {
            if (demoConfig.isDemoCardId(cardId = userWallet.cardId)) {
                sendDemo(
                    userWallet = userWallet,
                    network = network,
                    transactionData = txData,
                    signer = signer,
                )
            } else {
                val sendResult = transactionRepository.sendTransaction(
                    txData = txData,
                    signer = signer,
                    userWalletId = userWallet.walletId,
                    network = network,
                )
                when (sendResult) {
                    is Result.Failure -> handleError(sendResult).left()
                    is Result.Success -> sendResult.data.right()
                }
            }
        } catch (ex: Exception) {
            cardSdkConfigRepository.setLinkedTerminal(linkedTerminal)
            SendTransactionError.DataError(ex.message).left()
        }

        cardSdkConfigRepository.setLinkedTerminal(linkedTerminal)
        return sendResult.fold(
            ifRight = { result -> result.hash.right() },
            ifLeft = { it.left() },
        )
    }

    private suspend fun sendDemo(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Either<SendTransactionError, TransactionSendResult> {
        val demoTransactionSender = DemoTransactionSender(
            walletManagersFacade
                .getOrCreateWalletManager(userWallet.walletId, network)
                ?: error("WalletManager is null"),
        )

        val result = demoTransactionSender.send(transactionData = transactionData, signer = signer)

        return if (result is Result.Failure && result.error.customMessage.contains(DemoTransactionSender.ID)) {
            SendTransactionError.DemoCardError.left()
        } else {
            TransactionSendResult("hash").right()
        }
    }

    private fun handleError(result: Result.Failure): SendTransactionError {
        if (ResultChecker.isNetworkError(result)) {
            return SendTransactionError.NetworkError(
                code = result.error.message,
                message = result.error.customMessage,
            )
        }
        val error = result.error as? BlockchainSdkError ?: return SendTransactionError.UnknownError()
        return when (error) {
            is BlockchainSdkError.WrappedTangemError -> parseWrappedError(error)
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

    private fun parseWrappedError(error: BlockchainSdkError.WrappedTangemError): SendTransactionError {
        return if (error.code == USER_CANCELLED_ERROR_CODE) {
            SendTransactionError.UserCancelledError
        } else {
            when (val tangemError = error.tangemError) {
                is TangemSdkError -> {
                    val resource = tangemError.localizedDescriptionRes()
                    val resId = resource.resId ?: R.string.common_unknown_error
                    val resArgs = resource.args.map { it.value }
                    val textReference = resourceReference(resId, wrappedList(resArgs))
                    SendTransactionError.TangemSdkError(tangemError.code, textReference)
                }
                is BlockchainSdkError.WrappedTangemError -> {
                    parseWrappedError(tangemError) // todo remove when sdk errors are revised
                }
                else -> {
                    SendTransactionError.BlockchainSdkError(error.code, tangemError.customMessage)
                }
            }
        }
    }
}
