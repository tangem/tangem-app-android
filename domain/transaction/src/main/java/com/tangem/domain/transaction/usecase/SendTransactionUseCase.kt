package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.network.ResultChecker
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.common.TapWorkarounds.isStart2Coin
import com.tangem.domain.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.demo.DemoTransactionSender
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.parseWrappedError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet

class SendTransactionUseCase(
    private val demoConfig: DemoConfig,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val transactionRepository: TransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {
    suspend operator fun invoke(
        txsData: List<TransactionData>,
        userWallet: UserWallet,
        network: Network,
        sendMode: TransactionSender.MultipleTransactionSendMode,
    ): Either<SendTransactionError?, List<String>> {
        val card = userWallet.scanResponse.card
        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

        val signer = cardSdkConfigRepository.getCommonSigner(
            cardId = card.cardId.takeIf { isCardNotBackedUp },
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
        )

        val linkedTerminal = cardSdkConfigRepository.isLinkedTerminal()
        if (userWallet.scanResponse.card.isStart2Coin) {
            cardSdkConfigRepository.setLinkedTerminal(false)
        }
        val sendResult = try {
            if (demoConfig.isDemoCardId(cardId = userWallet.cardId)) {
                sendDemo(
                    userWallet = userWallet,
                    network = network,
                    transactionsData = txsData,
                    signer = signer,
                )
            } else {
                val sendResult = transactionRepository.sendMultipleTransactions(
                    txsData = txsData,
                    signer = signer,
                    userWalletId = userWallet.walletId,
                    network = network,
                    sendMode = sendMode,
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
        return sendResult
            .onRight {
                if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
                    singleNetworkStatusFetcher(
                        params = SingleNetworkStatusFetcher.Params(
                            userWalletId = userWallet.walletId,
                            network = network,
                        ),
                    )
                }
            }
            .fold(
                ifRight = { result -> result.hashes.right() },
                ifLeft = { it.left() },
            )
    }

    suspend operator fun invoke(
        txData: TransactionData,
        userWallet: UserWallet,
        network: Network,
    ): Either<SendTransactionError?, String> {
        return invoke(listOf(txData), userWallet, network, TransactionSender.MultipleTransactionSendMode.DEFAULT)
            .map { it.first() }
    }

    private suspend fun sendDemo(
        userWallet: UserWallet,
        network: Network,
        transactionsData: List<TransactionData>,
        signer: TransactionSigner,
    ): Either<SendTransactionError, TransactionsSendResult> {
        val demoTransactionSender = DemoTransactionSender(
            walletManagersFacade
                .getOrCreateWalletManager(userWallet.walletId, network)
                ?: error("WalletManager is null"),
        )

        val result = demoTransactionSender.sendMultiple(transactionDataList = transactionsData, signer = signer)

        return if (result is Result.Failure && result.error.customMessage.contains(DemoTransactionSender.ID)) {
            SendTransactionError.DemoCardError.left()
        } else {
            TransactionsSendResult(listOf("hash")).right()
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
                val minValue = minAmount.value?.format { simple(minAmount.decimals) }.orEmpty()
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