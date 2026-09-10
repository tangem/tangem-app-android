package com.tangem.datasource.local.markets

import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.JsonClass
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.store.datasource.markets.models.response.CoinCategoriesResponse.Category

internal class DefaultCoinCategoriesSnapshotStore(
    private val appPreferencesStore: AppPreferencesStore,
) : CoinCategoriesSnapshotStore {

    override suspend fun getSyncOrNull(country: String, environment: String): List<Category>? {
        val snapshot = appPreferencesStore.getObjectSyncOrNull<CoinCategoriesSnapshot>(COIN_CATEGORIES_SNAPSHOT_KEY)
            ?: return null

        val isSameOrigin = snapshot.country == country && snapshot.environment == environment

        return snapshot.categories.takeIf { isSameOrigin }
    }

    override suspend fun store(country: String, environment: String, categories: List<Category>) {
        appPreferencesStore.storeObject(
            key = COIN_CATEGORIES_SNAPSHOT_KEY,
            value = CoinCategoriesSnapshot(country = country, environment = environment, categories = categories),
        )
    }

    private companion object {
        val COIN_CATEGORIES_SNAPSHOT_KEY = stringPreferencesKey(name = "coinCategoriesSnapshot")
    }
}

@JsonClass(generateAdapter = true)
internal data class CoinCategoriesSnapshot(
    val country: String,
    val environment: String,
    val categories: List<Category>,
)