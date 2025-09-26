package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import java.math.BigDecimal

class YieldSupplyStartEarningUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    private val yieldSupplyErrorResolver: YieldSupplyErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        maxNetworkFee: BigDecimal,
    ): Either<YieldSupplyError, List<TransactionData.Uncompiled>> = Either.catch {
        yieldSupplyTransactionRepository.createEnterTransactions(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxNetworkFee = maxNetworkFee,
        )
    }.mapLeft(yieldSupplyErrorResolver::resolve)
}