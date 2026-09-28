package com.tangem.domain.promo.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoCampaign

class GetPromoCampaignsUseCase(
    private val repository: PromoRepository,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        forceRefresh: Boolean = false,
    ): Either<Throwable, List<PromoCampaign>> = Either.catch {
        repository.getCampaigns(userWallet, forceRefresh)
    }
}