package com.tangem.domain.earn.usecase

import arrow.core.Either
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.earn.EarnToken
import com.tangem.domain.models.earn.EarnType

class GetBestEarnOfferUseCase(
    private val repository: EarnRepository,
) {

    suspend operator fun invoke(type: EarnType): Either<Throwable, EarnToken?> = repository.getBestOffer(type)
}