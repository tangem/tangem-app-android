package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * [REDACTED_TODO_COMMENT]
     */
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): Either<Throwable, TransactionData> = withContext(dispatchers.io) {
        Either.catch {
            requireNotNull(
                transactionRepository.createTransaction(
                    amount = amount,
                    fee = fee,
                    memo = memo,
                    destination = destination,
                    userWalletId = userWalletId,
                    network = network,
                ),
            ) { "Failed to create transaction" }
        }
    }
}