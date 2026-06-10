package com.tangem.domain.yield.supply.promo.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.promo.YieldPromoRepository

class GetYieldBoostStatusUseCase(
    private val repository: YieldPromoRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        forceRefresh: Boolean = false,
    ): Either<Throwable, YieldBoostStatus> = Either.catch {
        repository.getYieldBoostStatus(userWalletId, forceRefresh)
    }
}