package com.tangem.data.settings

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.GeoResponse
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.GB_COUNTRY
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

@Suppress("TooManyFunctions")
internal class DefaultSettingsRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val appLogsStore: AppLogsStore,
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : SettingsRepository {

    private val userCountryFlow = MutableStateFlow<UserCountry?>(value = null)

    override suspend fun shouldShowSaveUserWalletScreen(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.SHOULD_SHOW_SAVE_USER_WALLET_SCREEN_KEY,
            default = true,
        )
    }

    override suspend fun setShouldShowSaveUserWalletScreen(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SHOULD_SHOW_SAVE_USER_WALLET_SCREEN_KEY, value = value)
    }

    override suspend fun isWalletScrollPreviewEnabled(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.WALLETS_SCROLL_PREVIEW_KEY,
            default = true,
        )
    }

    override suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean) {
        appPreferencesStore.store(
            key = PreferencesKeys.WALLETS_SCROLL_PREVIEW_KEY,
            value = isEnabled,
        )
    }

    override fun deleteDeprecatedLogs(maxSize: Int) {
        appLogsStore.deleteDeprecatedLogs(maxSize)
    }

    override suspend fun isSendTapHelpPreviewEnabled(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.SEND_TAP_HELP_PREVIEW_KEY,
            default = true,
        )
    }

    override suspend fun setSendTapHelpPreviewAvailability(isEnabled: Boolean) {
        appPreferencesStore.store(
            key = PreferencesKeys.SEND_TAP_HELP_PREVIEW_KEY,
            value = isEnabled,
        )
    }

    override suspend fun shouldOpenWelcomeScreenOnResume(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.SHOULD_OPEN_WELCOME_ON_RESUME_KEY,
            default = false,
        )
    }

    override suspend fun setShouldOpenWelcomeScreenOnResume(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SHOULD_OPEN_WELCOME_ON_RESUME_KEY, value = value)
    }

    override suspend fun shouldSaveAccessCodes(): Boolean {
        return appPreferencesStore.getSyncOrDefault(key = PreferencesKeys.SHOULD_SAVE_ACCESS_CODES_KEY, default = false)
    }

    override suspend fun setShouldSaveAccessCodes(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SHOULD_SAVE_ACCESS_CODES_KEY, value = value)
    }

    override suspend fun incrementAppLaunchCounter() {
        appPreferencesStore.editData { preferences ->
            val count = preferences.getOrDefault(key = PreferencesKeys.APP_LAUNCH_COUNT_KEY, default = 0)
            preferences[PreferencesKeys.APP_LAUNCH_COUNT_KEY] = count + 1
        }
    }

    override suspend fun shouldShowMarketsTooltip(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.SHOULD_SHOW_MARKETS_TOOLTIP_KEY,
            default = true,
        )
    }

    override suspend fun setMarketsTooltipShown(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.SHOULD_SHOW_MARKETS_TOOLTIP_KEY, value = !value)
    }

    override fun getUserCountryCodeSync(): UserCountry? {
        // If user country code is already set, return it
        val countryCode = userCountryFlow.value
        if (countryCode != null) return countryCode

        return null
    }

    override fun getUserCountryCode(): StateFlow<UserCountry?> = userCountryFlow

    override suspend fun fetchUserCountryCode() {
        Timber.i("Start fetching user country code")

        // for GB locale avoid request geo and use device default (FCA fixes)
        if (Locale.getDefault().country == GB_COUNTRY.code) {
            userCountryFlow.value = GB_COUNTRY
            return
        }

        withContext(dispatchers.io) {
            val country = runCatching { tangemTechApi.getUserCountryCode() }
                .fold(
                    onSuccess = GeoResponse::code,
                    onFailure = { Locale.getDefault().country },
                )
                .lowercase()

            val code = when (country) {
                UserCountry.Russia.code -> UserCountry.Russia
                else -> UserCountry.Other(code = country)
            }

            Timber.i("User code country is $code")

            userCountryFlow.value = code
        }
    }

    override suspend fun setGoogleServicesAvailability(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.IS_GOOGLE_SERVICES_AVAILABLE_KEY, value = value)
    }

    override suspend fun isGoogleServicesAvailability(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.IS_GOOGLE_SERVICES_AVAILABLE_KEY,
            default = false,
        )
    }

    override suspend fun setGooglePayAvailability(value: Boolean) {
        appPreferencesStore.store(key = PreferencesKeys.IS_GOOGLE_PAY_AVAILABLE_KEY, value = value)
    }

    override suspend fun isGooglePayAvailability(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.IS_GOOGLE_PAY_AVAILABLE_KEY,
            default = false,
        )
    }
}