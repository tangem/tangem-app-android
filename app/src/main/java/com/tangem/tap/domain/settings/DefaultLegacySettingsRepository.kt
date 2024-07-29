package com.tangem.tap.domain.settings

import com.tangem.domain.settings.repositories.LegacySettingsRepository
import com.tangem.tap.domain.sdk.TangemSdkManager

internal class DefaultLegacySettingsRepository(
    private val tangemSdkManager: TangemSdkManager,
) : LegacySettingsRepository {

    override suspend fun canUseBiometry(): Boolean = tangemSdkManager.checkCanUseBiometry()
}