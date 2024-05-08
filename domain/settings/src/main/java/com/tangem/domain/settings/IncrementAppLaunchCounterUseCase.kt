package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.SettingsRepository

class IncrementAppLaunchCounterUseCase(
    private val settingsRepository: SettingsRepository,
) {

    suspend operator fun invoke(): Either<Throwable, Unit> {
        return Either.catch { settingsRepository.incrementAppLaunchCounter() }
    }
}