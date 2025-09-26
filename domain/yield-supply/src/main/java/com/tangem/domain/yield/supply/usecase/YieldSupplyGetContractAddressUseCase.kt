package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository

class YieldSupplyGetContractAddressUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    private val yieldSupplyErrorResolver: YieldSupplyErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
    ): Either<YieldSupplyError, String?> = Either.catch {
        yieldSupplyTransactionRepository.getYieldContractAddress(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
    }.mapLeft(yieldSupplyErrorResolver::resolve)
}