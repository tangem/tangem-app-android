package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyError
import com.tangem.domain.yield.supply.YieldSupplyErrorResolver
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import java.math.BigDecimal

class YieldSupplyGetProtocolBalanceUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    private val yieldSupplyErrorResolver: YieldSupplyErrorResolver,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<YieldSupplyError, BigDecimal?> = Either.catch {
        requireNotNull(cryptoCurrency as? CryptoCurrency.Token)

        yieldSupplyTransactionRepository.getEffectiveProtocolBalance(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
    }.mapLeft(yieldSupplyErrorResolver::resolve)
}