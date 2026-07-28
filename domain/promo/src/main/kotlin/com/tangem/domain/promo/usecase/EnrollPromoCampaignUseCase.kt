package com.tangem.domain.promo.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.EnrollResult
import com.tangem.domain.promo.models.PromoCampaignId
import com.tangem.domain.promo.models.TokenReward

class EnrollPromoCampaignUseCase(
    private val repository: PromoRepository,
) {

    suspend operator fun invoke(
        campaign: PromoCampaignId,
        tokenReward: TokenReward,
        walletIds: List<UserWalletId>,
    ): Either<Throwable, EnrollResult> = Either.catch {
        repository.enroll(campaign, tokenReward, walletIds)
    }
}