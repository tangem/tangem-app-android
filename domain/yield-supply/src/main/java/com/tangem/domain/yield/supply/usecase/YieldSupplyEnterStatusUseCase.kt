package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyEnterStatus

class YieldSupplyEnterStatusUseCase(
    private val yieldSupplyRepository: YieldSupplyRepository,
) {

    operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, YieldSupplyEnterStatus?> {
        return Either.catch {
            yieldSupplyRepository.getTokenProtocolStatus(
                userWalletId,
                cryptoCurrencyStatus.currency,
            )
        }
    }
}