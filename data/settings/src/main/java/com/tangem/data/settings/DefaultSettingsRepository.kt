package com.tangem.data.settings

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.logs.AppLogsStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
internal class DefaultSettingsRepository(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
    private val appLogsStore: AppLogsStore,
    private val tangemTechApi: TangemTechApi,
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

        withContext(dispatchers.io) {
            val geoCode = runSuspendCatching { tangemTechApi.getUserCountryCode() }
                .getOrNull()
                ?.code
                ?.takeIf(String::isNotBlank)

            val country = if (geoCode == null) {
                getCachedUserCountry() ?: UserCountry.fromDeviceLocale()
            } else {
                UserCountry.from(code = geoCode)
            }

            TangemLogger.i("User code country is $country")

            userCountryFlow.value = country

            if (geoCode != null) cacheUserCountryCode(country)
        }
    }

    /** Consumers wait for a published country, so a failed read must fall through to the device region */
    private suspend fun getCachedUserCountry(): UserCountry? {
        return runSuspendCatching {
            appPreferencesStore.getSyncOrNull(key = storePreferencesKeys.geoUserCountryCodeKey)
        }
            .onFailure { TangemLogger.e("Unable to read the cached user country code", it) }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
            ?.let(UserCountry::from)
    }

    /** Caching must not discard an already resolved country, so a failed write is only reported */
    private suspend fun cacheUserCountryCode(country: UserCountry) {
        runSuspendCatching {
            appPreferencesStore.store(key = storePreferencesKeys.geoUserCountryCodeKey, value = country.code)
        }
            .onFailure { TangemLogger.e("Unable to cache the user country code", it) }
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