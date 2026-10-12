package com.tangem.domain.stories

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.stories.models.StoryContent
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class GetStoryContentUseCase(
    private val storiesRepository: StoriesRepository,
    private val settingsRepository: SettingsRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(id: String): Flow<Either<Throwable, StoryContent?>> {
        // the story flow never completes, so a later FCA decision must cancel it instead of queueing behind it
        return isFCAAllowed(id).flatMapLatest { isAllowed ->
            if (isAllowed) {
                storiesRepository.getStoryById(id)
                    .map<StoryContent?, Either<Throwable, StoryContent?>> { it.right() }
                    .catch { emit(it.left()) }
                    .onEmpty { emit(null.right()) }
            } else {
                flowOf(null.right())
            }
        }
    }

    suspend fun invokeSync(id: String, refresh: Boolean = false): Either<Throwable, StoryContent?> = Either.catch {
        return@catch if (isFCAAllowedSync(id)) {
            storiesRepository.getStoryByIdSync(id, refresh)
        } else {
            null
        }
    }

    private fun isFCAAllowed(id: String): Flow<Boolean> {
        return if (id == StoryContentIds.STORY_FIRST_TIME_SWAP.id) {
            settingsRepository.getUserCountryCode()
                .map { userCountry -> !userCountry.needApplyFCARestrictions() }
                .distinctUntilChanged()
        } else {
            flowOf(true)
        }
    }

    /**
     * Awaits the backend-resolved country for [USER_COUNTRY_TIMEOUT] and falls back to the device locale:
     * a country that could not be resolved must never lift an FCA restriction.
     */
    private suspend fun isFCAAllowedSync(id: String): Boolean {
        if (id != StoryContentIds.STORY_FIRST_TIME_SWAP.id) return true

        val userCountry = withTimeoutOrNull(USER_COUNTRY_TIMEOUT) {
            settingsRepository.getUserCountryCode().filterNotNull().first()
        }

        return !userCountry.needApplyFCARestrictions()
    }

    private companion object {
        val USER_COUNTRY_TIMEOUT = 3.seconds
    }
}