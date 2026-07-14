package com.tangem.domain.marketing

import arrow.core.Either

class DismissMarketingBannerUseCase(
    private val repository: MarketingRepository,
) {

    suspend operator fun invoke(campaignId: Int): Either<Throwable, Unit> = Either.catch {
        repository.dismissBanner(campaignId)
    }
}