package com.tangem.domain.promo

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.promo.models.StoryContent
import com.tangem.domain.promo.models.StoryContentIds
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

class GetStoryContentUseCase(
    private val promoRepository: PromoRepository,
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(id: String): Flow<Either<Throwable, StoryContent?>> {
        return isFCAAllowed(id).transform { isAllowed ->
            if (isAllowed) {
                emitAll(
                    promoRepository.getStoryById(id)
                        .map<StoryContent?, Either<Throwable, StoryContent?>> { it.right() }
                        .catch { emit(it.left()) }
                        .onEmpty { emit(null.right()) },
                )
            } else {
                emit(null.right())
            }
        }
    }

    suspend fun invokeSync(id: String, refresh: Boolean = false): Either<Throwable, StoryContent?> = Either.catch {
        val isFCAAllowed = isFCAAllowed(id).firstOrNull() ?: false
        return@catch if (isFCAAllowed) {
            promoRepository.getStoryByIdSync(id, refresh)
        } else {
            null
        }
    }

    private fun isFCAAllowed(id: String): Flow<Boolean> {
        return if (id == StoryContentIds.STORY_FIRST_TIME_SWAP.id) {
            settingsRepository.getUserCountryCode()
                .filterNotNull()
                .timeout(5.seconds)
                .map { !it.needApplyFCARestrictions() }
        } else {
            flowOf(true)
        }
    }
}