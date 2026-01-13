package com.tangem.datasource.local.appsflyer

import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.wallets.models.AppsFlyerConversionData
import timber.log.Timber

internal class DefaultAppsFlyerConversionStore(
    private val appPreferencesStore: AppPreferencesStore,
) : AppsFlyerConversionStore {

    override suspend fun get(): AppsFlyerConversionData? {
        val dto = appPreferencesStore.getObjectSyncOrNull<ConversionDataDTO>(KEY) ?: return null

        return ConversionDataConverter.convertBack(value = dto).also {
            Timber.i("Getting conversion data from store: $it")
        }
    }

    override suspend fun store(value: AppsFlyerConversionData) {
        Timber.i("Storing conversion data to store: $value")

        val dto = ConversionDataConverter.convert(value)

        appPreferencesStore.storeObject(KEY, dto)
    }

    private companion object {

        val KEY = stringPreferencesKey("APPS_FLYER_CONVERSION_DATA")
    }
}

@JsonClass(generateAdapter = true)
internal data class ConversionDataDTO(
    @Json(name = "refcode") val refcode: String,
    @Json(name = "campaign") val campaign: String?,
)