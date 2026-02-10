package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.LegacySettingsRepository

class CanUseBiometryUseCase(private val legacySettingsRepository: LegacySettingsRepository) {

    @Deprecated("You probably want to use strict() instead. Check implementation", ReplaceWith("strict()"))
    suspend operator fun invoke(): Boolean = legacySettingsRepository.canUseBiometry()

    suspend fun strict(): Boolean = legacySettingsRepository.canUseBiometryStrict()
}