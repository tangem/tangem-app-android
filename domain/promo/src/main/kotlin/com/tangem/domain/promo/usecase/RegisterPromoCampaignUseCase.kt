package com.tangem.domain.promo.usecase

import arrow.core.Either
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.promo.models.PromoEnrollment
import com.tangem.domain.promo.models.PromoRegistrationError
import com.tangem.domain.promo.models.PromoRegistrationResult

class RegisterPromoCampaignUseCase(
    private val repository: PromoRepository,
) {

    suspend operator fun invoke(
        promotionId: Int,
        enrollments: List<PromoEnrollment>,
    ): Either<PromoRegistrationError, List<PromoRegistrationResult>> {
        return repository.register(promotionId, enrollments)
    }
}