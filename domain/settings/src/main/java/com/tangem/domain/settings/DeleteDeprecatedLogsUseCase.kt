package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository

class DeleteDeprecatedLogsUseCase(
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke() {
        settingsRepository.deleteDeprecatedLogs(maxSize = MAX_LOGS_SIZE)
    }

    private companion object {
        const val MAX_LOGS_SIZE = 24_900_000 // ≈ 25 MB
    }
}