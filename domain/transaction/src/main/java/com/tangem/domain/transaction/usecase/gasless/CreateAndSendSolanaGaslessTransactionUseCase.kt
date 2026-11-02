package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.domain.transaction.error.SolanaGaslessSendError
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.logging.TangemLogger

/**
 * Signs a provider-built Solana transaction with the backend's [feePayer] at index 0 and hands it to the gasless
 * service, which co-signs and broadcasts it. Returns the on-chain signature.
 */
class CreateAndSendSolanaGaslessTransactionUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val solanaGaslessTransactionRepository: SolanaGaslessTransactionRepository,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val gaslessSignerProvider: GaslessSignerProvider,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        network: Network,
        transactionData: TransactionData,
        feePayer: String,
    ): Either<SolanaGaslessSendError, String> = either {
        val signed = when (
            val result = walletManagersFacade.signSolanaGaslessTransaction(
                userWalletId = userWallet.walletId,
                network = network,
                transactionData = transactionData,
                feePayer = feePayer,
                signer = gaslessSignerProvider(userWallet),
            )
        ) {
            is Result.Success -> result.data
            is Result.Failure -> raise(SolanaGaslessSendError.Signing(SendTransactionUseCase.handleError(result)))
            null -> raise(SolanaGaslessSendError.Signing(SendTransactionError.UnknownError()))
        }

        val signature = solanaGaslessTransactionRepository.submit(
            userWalletId = userWallet.walletId,
            transaction = signed.transaction,
            lastValidBlockHeight = signed.lastValidBlockHeight,
        )
            .onLeft { error ->
                if (error is SolanaGaslessError.MaybeSent) {
                    refreshNetworkStatus(userWalletId = userWallet.walletId, network = network)
                }
            }
            .mapLeft(SolanaGaslessSendError::Service)
            .bind()

        refreshNetworkStatus(userWalletId = userWallet.walletId, network = network)
        signature
    }

    /**
     * Same refresh [SendTransactionUseCase] does after a broadcast, also run when the tx may have been broadcast. It
     * surfaces the pending transaction: Solana's wallet update reads in-flight signatures from the RPC, while
     * `Wallet.addOutgoingTransaction` ignores compiled transactions.
     */
    private suspend fun refreshNetworkStatus(userWalletId: UserWalletId, network: Network) {
        singleNetworkStatusFetcher(SingleNetworkStatusFetcher.Params(userWalletId = userWalletId, network = network))
            .onLeft { TangemLogger.e("Failed to refresh Solana network status after sponsored transaction", it) }
    }
}