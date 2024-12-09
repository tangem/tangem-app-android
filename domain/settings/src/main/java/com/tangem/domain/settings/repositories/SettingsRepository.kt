package com.tangem.domain.settings.repositories

import com.tangem.domain.settings.usercountry.models.UserCountry

@Suppress("TooManyFunctions")
interface SettingsRepository {

    suspend fun shouldShowSaveUserWalletScreen(): Boolean

    suspend fun setShouldShowSaveUserWalletScreen(value: Boolean)

    suspend fun isWalletScrollPreviewEnabled(): Boolean

    suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean)

    fun deleteDeprecatedLogs(maxSize: Int)

    suspend fun isSendTapHelpPreviewEnabled(): Boolean

    suspend fun setSendTapHelpPreviewAvailability(isEnabled: Boolean)

    suspend fun wasApplicationStopped(): Boolean

    suspend fun setWasApplicationStopped(value: Boolean)

    suspend fun shouldOpenWelcomeScreenOnResume(): Boolean

    suspend fun setShouldOpenWelcomeScreenOnResume(value: Boolean)

    suspend fun shouldSaveAccessCodes(): Boolean

    suspend fun setShouldSaveAccessCodes(value: Boolean)

    suspend fun incrementAppLaunchCounter()

    suspend fun shouldShowMarketsTooltip(): Boolean

    suspend fun setMarketsTooltipShown(value: Boolean)

    suspend fun getUserCountryCodeSync(): UserCountry?

    suspend fun fetchUserCountryCode()

    suspend fun setGoogleServicesAvailability(value: Boolean)

    suspend fun isGoogleServicesAvailability(): Boolean

    suspend fun setGooglePayAvailability(value: Boolean)

    suspend fun isGooglePayAvailability(): Boolean
}