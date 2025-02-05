package com.tangem.domain.promo

import arrow.core.Either
import com.tangem.domain.promo.models.StoryContent

class GetStoryContentUseCase(
    private val promoRepository: PromoRepository,
) {

    suspend operator fun invoke(id: String): Either<Throwable, StoryContent> = Either.catch {
        promoRepository.getStoryById(id)
    }
}