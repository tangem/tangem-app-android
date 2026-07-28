package com.tangem.domain.promo.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.PromoCampaignState

class GetPromoCampaignStateUseCase(
    private val repository: PromoRepository,
) {

    suspend operator fun invoke(
        campaign: PromoCampaignId,
        userWalletId: UserWalletId,
        forceRefresh: Boolean = false,
    ): Either<Throwable, PromoCampaignState> = Either.catch {
        repository.getCampaignState(campaign, userWalletId, forceRefresh)
    }
}