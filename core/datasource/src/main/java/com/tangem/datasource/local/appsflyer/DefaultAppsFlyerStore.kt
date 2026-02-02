package com.tangem.datasource.local.appsflyer

import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import timber.log.Timber

internal class DefaultAppsFlyerStore(
    private val appPreferencesStore: AppPreferencesStore,
) : AppsFlyerStore {

    override suspend fun get(): AppsFlyerConversionData? {
        val dto = appPreferencesStore.getObjectSyncOrNull<ConversionDataDTO>(CONVERSION_DATA_KEY) ?: return null

        return ConversionDataConverter.convertBack(value = dto).also {
            Timber.i("Getting conversion data from store: $it")
        }
    }

    override suspend fun getUID(): String? = appPreferencesStore.getSyncOrNull(UID_KEY)

    override suspend fun store(value: AppsFlyerConversionData) {
        Timber.i("Storing conversion data to store: $value")

        val dto = ConversionDataConverter.convert(value)

        appPreferencesStore.storeObject(CONVERSION_DATA_KEY, dto)
    }

    override suspend fun storeIfAbsent(value: AppsFlyerConversionData) {
        Timber.i("Storing conversion data to store if absent: $value")
        appPreferencesStore.editData { preferences ->
            val saved = preferences[CONVERSION_DATA_KEY]

            if (saved == null) {
                Timber.i("Conversion data is absent, storing $value")
                preferences.setObject(CONVERSION_DATA_KEY, ConversionDataConverter.convert(value))
            }
        }
    }

    override suspend fun storeUIDIfAbsent(value: String) {
        Timber.i("Storing UID to store if absent: $value")
        appPreferencesStore.editData { preferences ->
            val saved = preferences[UID_KEY]

            if (saved == null) {
                Timber.i("UID is absent, storing $value")
                preferences[UID_KEY] = value
            }
        }
    }

    private companion object {

        val UID_KEY = stringPreferencesKey("APPS_FLYER_UID")
        val CONVERSION_DATA_KEY = stringPreferencesKey("APPS_FLYER_CONVERSION_DATA")
    }
}

@JsonClass(generateAdapter = true)
internal data class ConversionDataDTO(
    @Json(name = "refcode") val refcode: String,
    @Json(name = "campaign") val campaign: String?,
)