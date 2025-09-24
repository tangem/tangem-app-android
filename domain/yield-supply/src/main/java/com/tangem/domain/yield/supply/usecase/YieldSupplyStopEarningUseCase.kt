package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository

class YieldSupplyStopEarningUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        fee: Fee?,
    ): Either<Throwable, TransactionData.Uncompiled> = Either.catch {
        yieldSupplyTransactionRepository.createExitTransaction(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            fee = null,
        )
    }
}