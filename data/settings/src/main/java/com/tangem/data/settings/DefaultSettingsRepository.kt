package com.tangem.data.settings

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.GeoResponse
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.SettingsFeatureToggles
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.GB_COUNTRY
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

@Suppress("TooManyFunctions")
internal class DefaultSettingsRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
    private val appLogsStore: AppLogsStore,
    private val tangemTechApi: TangemTechApi,
    private val settingsFeatureToggles: SettingsFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) : SettingsRepository {

    private val userCountryFlow = MutableStateFlow<UserCountry?>(value = null)

    override suspend fun shouldShowAskBiometry(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.shouldShowAskBiometryKey,
            default = true,
        )
    }

    override suspend fun setShouldShowAskBiometry(value: Boolean) {
        appPreferencesStore.store(key = storePreferencesKeys.shouldShowAskBiometryKey, value = value)
    }

    override suspend fun isWalletScrollPreviewEnabled(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.walletsScrollPreviewKey,
            default = true,
        )
    }

    override suspend fun setWalletScrollPreviewAvailability(isEnabled: Boolean) {
        appPreferencesStore.store(
            key = storePreferencesKeys.walletsScrollPreviewKey,
            value = isEnabled,
        )
    }

    override fun deleteDeprecatedLogs(maxSize: Int) {
        appLogsStore.deleteDeprecatedLogs(maxSize)
    }

    override fun isSendTapHelpPreviewEnabled(): Flow<Boolean> {
        return appPreferencesStore.get(
            key = storePreferencesKeys.sendTapHelpPreviewKey,
            default = true,
        )
    }

    override suspend fun isSendTapHelpPreviewEnabledSync(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.sendTapHelpPreviewKey,
            default = true,
        )
    }

    override suspend fun setSendTapHelpPreviewAvailability(isEnabled: Boolean) {
        appPreferencesStore.store(
            key = storePreferencesKeys.sendTapHelpPreviewKey,
            value = isEnabled,
        )
    }

    override suspend fun shouldOpenWelcomeScreenOnResume(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.shouldOpenWelcomeOnResumeKey,
            default = false,
        )
    }

    override suspend fun setShouldOpenWelcomeScreenOnResume(value: Boolean) {
        appPreferencesStore.store(key = storePreferencesKeys.shouldOpenWelcomeOnResumeKey, value = value)
    }

    override suspend fun shouldSaveAccessCodes(): Boolean {
        return appPreferencesStore.getSyncOrNull(key = storePreferencesKeys.requireAccessCodeKey)?.not()
            ?: appPreferencesStore.getSyncOrDefault(
                key = storePreferencesKeys.shouldSaveAccessCodesKey,
                default = false,
            )
    }

    override suspend fun setShouldSaveAccessCodes(value: Boolean) {
        appPreferencesStore.store(key = storePreferencesKeys.shouldSaveAccessCodesKey, value = value)
        appPreferencesStore.store(key = storePreferencesKeys.requireAccessCodeKey, value = value.not())
    }

    override suspend fun incrementAppLaunchCounter() {
        appPreferencesStore.editData { preferences ->
            val count = preferences.getOrDefault(key = storePreferencesKeys.appLaunchCountKey, default = 0)
            preferences[storePreferencesKeys.appLaunchCountKey] = count + 1
        }
    }

    override suspend fun getWalletFirstUsageDate(): Long {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.walletFirstUsageDateKey,
            default = 0L,
        )
    }

    override suspend fun setWalletFirstUsageDate(value: Long) {
        appPreferencesStore.store(key = storePreferencesKeys.walletFirstUsageDateKey, value = value)
    }

    override suspend fun shouldShowMarketsTooltip(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.shouldShowMarketsTooltipKey,
            default = true,
        )
    }

    override suspend fun setMarketsTooltipShown(value: Boolean) {
        appPreferencesStore.store(key = storePreferencesKeys.shouldShowMarketsTooltipKey, value = !value)
    }

    override fun getUserCountryCodeSync(): UserCountry? = userCountryFlow.value

    override fun getUserCountryCode(): StateFlow<UserCountry?> = userCountryFlow

    override suspend fun fetchUserCountryCode() {
        TangemLogger.i("Start fetching user country code")

        if (!settingsFeatureToggles.isGeoCountryCacheFallbackEnabled) {
            fetchUserCountryCodeLegacy()
            return
        }

        withContext(dispatchers.io) {
            val geoCode = runSuspendCatching { tangemTechApi.getUserCountryCode() }
                .getOrNull()
                ?.code
                ?.takeIf(String::isNotBlank)

            val country = if (geoCode == null) {
                appPreferencesStore.getSyncOrNull(key = storePreferencesKeys.geoUserCountryCodeKey)
                    ?.takeIf(String::isNotBlank)
                    ?.let(UserCountry::from)
                    ?: UserCountry.fromDeviceLocale()
            } else {
                UserCountry.from(code = geoCode)
            }

            TangemLogger.i("User code country is $country")

            userCountryFlow.value = country

            if (geoCode != null) cacheUserCountryCode(country)
        }
    }

    /** Caching must not discard an already resolved country, so a failed write is only reported */
    private suspend fun cacheUserCountryCode(country: UserCountry) {
        runSuspendCatching {
            appPreferencesStore.store(key = storePreferencesKeys.geoUserCountryCodeKey, value = country.code)
        }
            .onFailure { TangemLogger.e("Unable to cache the user country code", it) }
    }

    private suspend fun fetchUserCountryCodeLegacy() {
        // for GB locale avoid request geo and use device default (FCA fixes)
        if (Locale.getDefault().country == GB_COUNTRY.code) {
            TangemLogger.i("User code country is $GB_COUNTRY")

            userCountryFlow.value = GB_COUNTRY
            return
        }

        withContext(dispatchers.io) {
            val code = runSuspendCatching { tangemTechApi.getUserCountryCode() }
                .fold(
                    onSuccess = GeoResponse::code,
                    onFailure = { Locale.getDefault().country },
                )

            val country = UserCountry.from(code = code)

            TangemLogger.i("User code country is $country")

            userCountryFlow.value = country
        }
    }

    override suspend fun setGoogleServicesAvailability(value: Boolean) {
        appPreferencesStore.store(key = storePreferencesKeys.isGoogleServicesAvailableKey, value = value)
    }

    override suspend fun isGoogleServicesAvailability(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.isGoogleServicesAvailableKey,
            default = false,
        )
    }

    override suspend fun setGooglePayAvailability(value: Boolean) {
        appPreferencesStore.store(key = storePreferencesKeys.isGooglePayAvailableKey, value = value)
    }

    override suspend fun isGooglePayAvailability(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.isGooglePayAvailableKey,
            default = false,
        )
    }

    override suspend fun isRootDetectedWarningShown(): Boolean {
        return appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.rootDetectedWarningShownKey,
            default = false,
        )
    }

    override suspend fun setRootDetectedWarningShown(value: Boolean) {
        appPreferencesStore.store(key = storePreferencesKeys.rootDetectedWarningShownKey, value = value)
    }
}