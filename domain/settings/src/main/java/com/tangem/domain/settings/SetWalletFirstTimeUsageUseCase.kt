package com.tangem.domain.settings

import arrow.core.Either
import com.tangem.domain.settings.repositories.SettingsRepository
import java.util.Calendar

class SetWalletFirstTimeUsageUseCase(private val settingsRepository: SettingsRepository) {

    suspend operator fun invoke() = Either.catch {
        val savedTime = settingsRepository.getWalletFirstUsageDate()

        if (savedTime == 0L) {
            settingsRepository.setWalletFirstUsageDate(Calendar.getInstance().timeInMillis)
        }
    }
}