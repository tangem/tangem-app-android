package com.tangem.domain.promo

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.promo.models.StoryContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty

class GetStoryContentUseCase(
    private val promoRepository: PromoRepository,
) {

    operator fun invoke(id: String): Flow<Either<Throwable, StoryContent?>> = promoRepository.getStoryById(id)
        .map<StoryContent?, Either<Throwable, StoryContent?>> { it.right() }
        .catch { emit(it.left()) }
        .onEmpty { emit(null.right()) }

    suspend fun invokeSync(id: String): Either<Throwable, StoryContent?> = Either.catch {
        promoRepository.getStoryByIdSync(id)
    }
}