package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository

class YieldSupplyActivateUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        address: String,
    ): Either<Throwable, Boolean> = Either.catch {
        val token = cryptoCurrency as? CryptoCurrency.Token ?: error("Token expected")
        yieldSupplyRepository.activateProtocol(
            userWalletId = userWalletId,
            cryptoCurrencyToken = token,
            address = address,
        )
    }
}