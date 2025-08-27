package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.TransactionRepository

class ValidateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        amount: Amount,
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): Either<Throwable, Unit> = Either.catch {
        transactionRepository.validateTransaction(
            amount = amount,
            fee = fee,
            memo = memo,
            destination = destination,
            userWalletId = userWalletId,
            network = network,
        ).fold(onSuccess = { Unit.right() }, onFailure = { it.left() })
    }
}