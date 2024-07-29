package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.LegacySettingsRepository

class CanUseBiometryUseCase(private val legacySettingsRepository: LegacySettingsRepository) {

    suspend operator fun invoke(): Boolean = legacySettingsRepository.canUseBiometry()
}