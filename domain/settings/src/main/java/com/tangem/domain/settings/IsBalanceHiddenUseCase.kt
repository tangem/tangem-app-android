package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IsBalanceHiddenUseCase(
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(): Flow<Boolean> {
        return settingsRepository.isBalanceHiddenEvents().map {
            it.isBalanceHidden
        }
    }

}
