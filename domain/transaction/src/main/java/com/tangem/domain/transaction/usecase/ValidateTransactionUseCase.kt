package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.wallets.models.UserWalletId

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
        isSwap: Boolean = false,
        hash: String? = null,
    ): Either<Throwable, Unit> {
        return transactionRepository.validateTransaction(
            amount = amount,
            fee = fee,
            memo = memo,
            destination = destination,
            userWalletId = userWalletId,
            network = network,
            isSwap = isSwap,
            txExtras = null,
            hash = hash,
        )
            .fold(onSuccess = { Unit.right() }, onFailure = { it.left() })
    }
}