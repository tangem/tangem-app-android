package com.tangem.domain.transaction.usecase.gasless

import arrow.core.Either
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.isGasless

/**
 * Sends a transaction whose fee a relayer fronts. Mirrors the fee side, where [GetFeeForGaslessUseCase]
 * dispatches over the protocol resolvers: whichever produced the fee also sends it.
 */
class CreateAndSendGaslessTransactionUseCase(
    private val createAndSendEvmGaslessTransactionUseCase: CreateAndSendEvmGaslessTransactionUseCase,
    private val createAndSendTronGaslessTransactionUseCase: CreateAndSendTronGaslessTransactionUseCase,
) {

    /**
     * @param fee must be gasless — a native fee is a call-site bug and is rejected rather than sent, as
     * the EVM flow would otherwise fail deep inside signing.
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        transactionData: TransactionData,
        fee: TransactionFeeExtended,
    ): Either<SendTransactionError, String> {
        return when {
            !fee.isGasless -> SendTransactionError.DataError("Gasless send called with a native fee").left()
            fee.tronGaslessQuote != null -> createAndSendTronGaslessTransactionUseCase(
                userWallet = userWallet,
                transactionData = transactionData,
                fee = fee,
            )
            else -> createAndSendEvmGaslessTransactionUseCase(
                userWallet = userWallet,
                transactionData = transactionData,
                fee = fee,
            )
        }
    }
}