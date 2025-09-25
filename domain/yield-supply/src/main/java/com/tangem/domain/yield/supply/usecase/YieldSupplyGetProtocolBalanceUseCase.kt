package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import java.math.BigDecimal

class YieldSupplyGetProtocolBalanceUseCase(
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Either<Throwable, BigDecimal?> = Either.catch {
        requireNotNull(cryptoCurrency as CryptoCurrency.Token)

        yieldSupplyTransactionRepository.getProtocolBalance(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
    }
}