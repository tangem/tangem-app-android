package com.tangem.datasource.local.appsflyer

import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DefaultAppsFlyerStore(
    private val appPreferencesStore: AppPreferencesStore,
) : AppsFlyerStore {

    override suspend fun get(): AppsFlyerConversionData? {
        val dto = appPreferencesStore.getObjectSyncOrNull<ConversionDataDTO>(CONVERSION_DATA_KEY) ?: return null

        return ConversionDataConverter.convertBack(value = dto).also {
            TangemLogger.i("Getting conversion data from store: $it")
        }
    }

    override suspend fun getUID(): String? = appPreferencesStore.getSyncOrNull(UID_KEY)

    override suspend fun store(value: AppsFlyerConversionData) {
        TangemLogger.i("Storing conversion data to store: $value")

        val dto = ConversionDataConverter.convert(value)

        appPreferencesStore.storeObject(CONVERSION_DATA_KEY, dto)
    }

    override suspend fun storeIfAbsent(value: AppsFlyerConversionData) {
        TangemLogger.i("Storing conversion data to store if absent: $value")
        appPreferencesStore.editData { preferences ->
            val saved = preferences[CONVERSION_DATA_KEY]

            if (saved == null) {
                TangemLogger.i("Conversion data is absent, storing $value")
                preferences.setObject(CONVERSION_DATA_KEY, ConversionDataConverter.convert(value))
            }
        }
    }

    override suspend fun storeUIDIfAbsent(value: String) {
        TangemLogger.i("Storing UID to store if absent: $value")
        appPreferencesStore.editData { preferences ->
            val saved = preferences[UID_KEY]

            if (saved == null) {
                TangemLogger.i("UID is absent, storing $value")
                preferences[UID_KEY] = value
            }
        }
    }

    override fun observeNavigationDeeplink(): Flow<String?> = appPreferencesStore.data
        .map { preferences -> preferences[NAVIGATION_DEEPLINK_KEY] }
        .distinctUntilChanged()

    override suspend fun getNavigationDeeplink(): String? = appPreferencesStore.getSyncOrNull(NAVIGATION_DEEPLINK_KEY)

    override suspend fun storeNavigationDeeplink(deepLinkValue: String) {
        appPreferencesStore.editData { preferences ->
            preferences[NAVIGATION_DEEPLINK_KEY] = deepLinkValue
        }
    }

    override suspend fun clearNavigationDeeplink() {
        appPreferencesStore.editData { preferences ->
            preferences.remove(NAVIGATION_DEEPLINK_KEY)
        }
    }

    private companion object {
        val UID_KEY = stringPreferencesKey("APPS_FLYER_UID")
        val CONVERSION_DATA_KEY = stringPreferencesKey("APPS_FLYER_CONVERSION_DATA")
        val NAVIGATION_DEEPLINK_KEY = stringPreferencesKey("appsflyer_navigation_deeplink")
    }
}

@JsonClass(generateAdapter = true)
internal data class ConversionDataDTO(
    @Json(name = "refcode") val refcode: String,
    @Json(name = "campaign") val campaign: String?,
)