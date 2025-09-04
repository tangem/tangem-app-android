package com.tangem.domain.settings.repositories

import com.tangem.domain.settings.usercountry.models.UserCountry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Suppress("TooManyFunctions")
interface SettingsRepository {

    suspend fun shouldShowSaveUserWalletScreen(): Boolean

    suspend fun setShouldShowSaveUserWalletScreen(value: Boolean)

    suspend fun isWalletScrollPreviewEnabled(): Boolean

    suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean)

    fun deleteDeprecatedLogs(maxSize: Int)

    fun isSendTapHelpPreviewEnabled(): Flow<Boolean>

    suspend fun isSendTapHelpPreviewEnabledSync(): Boolean

    suspend fun setSendTapHelpPreviewAvailability(isEnabled: Boolean)

    suspend fun shouldOpenWelcomeScreenOnResume(): Boolean

    suspend fun setShouldOpenWelcomeScreenOnResume(value: Boolean)

    @Deprecated("Use walletsRepository.requireAccessCode instead")
    suspend fun shouldSaveAccessCodes(): Boolean

    @Deprecated("Use walletsRepository.requireAccessCode instead")
    suspend fun setShouldSaveAccessCodes(value: Boolean)

    suspend fun incrementAppLaunchCounter()

    suspend fun getWalletFirstUsageDate(): Long

    suspend fun setWalletFirstUsageDate(value: Long)

    suspend fun shouldShowMarketsTooltip(): Boolean

    suspend fun setMarketsTooltipShown(value: Boolean)

    fun getUserCountryCodeSync(): UserCountry?

    fun getUserCountryCode(): StateFlow<UserCountry?>

    suspend fun fetchUserCountryCode()

    suspend fun setGoogleServicesAvailability(value: Boolean)

    suspend fun isGoogleServicesAvailability(): Boolean

    suspend fun setGooglePayAvailability(value: Boolean)

    suspend fun isGooglePayAvailability(): Boolean
}