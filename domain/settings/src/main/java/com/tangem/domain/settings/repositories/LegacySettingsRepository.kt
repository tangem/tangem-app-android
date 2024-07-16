package com.tangem.domain.settings.repositories

interface LegacySettingsRepository {

    suspend fun canUseBiometry(): Boolean
}
