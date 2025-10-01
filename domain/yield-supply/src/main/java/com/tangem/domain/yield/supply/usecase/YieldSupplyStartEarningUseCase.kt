package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository

class YieldSupplyStartEarningUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, List<TransactionData.Uncompiled>> = Either.catch {
        yieldSupplyTransactionRepository.createEnterTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
    }
}