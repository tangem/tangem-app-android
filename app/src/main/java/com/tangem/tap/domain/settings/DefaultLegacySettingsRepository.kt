package com.tangem.tap.domain.settings

import com.tangem.domain.settings.repositories.LegacySettingsRepository

internal class DefaultLegacySettingsRepository(
    private val tangemSdkManager: TangemSdkManager,
) : LegacySettingsRepository {

    override suspend fun canUseBiometry(): Boolean = tangemSdkManager.checkCanUseBiometry()
}
