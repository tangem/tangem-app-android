package com.tangem.domain.settings.repositories

interface SettingsRepository {

    suspend fun shouldShowSaveUserWalletScreen(): Boolean

    suspend fun setShouldShowSaveUserWalletScreen(value: Boolean)

    suspend fun isWalletScrollPreviewEnabled(): Boolean

    suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean)

    @Throws
    fun saveLogMessage(message: String)

    @Throws
    suspend fun deleteDeprecatedLogs(maxSize: Int)

    suspend fun isSendTapHelpPreviewEnabled(): Boolean

    suspend fun setSendTapHelpPreviewAvailability(isEnabled: Boolean)

    suspend fun wasApplicationStopped(): Boolean

    suspend fun setWasApplicationStopped(value: Boolean)

    suspend fun shouldOpenWelcomeScreenOnResume(): Boolean

    suspend fun setShouldOpenWelcomeScreenOnResume(value: Boolean)

    suspend fun shouldSaveAccessCodes(): Boolean

    suspend fun setShouldSaveAccessCodes(value: Boolean)

    suspend fun incrementAppLaunchCounter()
}
