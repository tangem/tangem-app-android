package com.tangem.data.marketing

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.domain.marketing.PromoPageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class DefaultPromoPageRepository(
    private val appPreferencesStore: AppPreferencesStore,
) : PromoPageRepository {

    override fun observePage(): Flow<String?> = appPreferencesStore.get(PROMO_PAGE_KEY).distinctUntilChanged()

    override suspend fun setPage(page: String?) {
        appPreferencesStore.editData { preferences ->
            if (page == null) {
                preferences.remove(PROMO_PAGE_KEY)
            } else {
                preferences[PROMO_PAGE_KEY] = page
            }
        }
    }

    override suspend fun consumePage(): String? {
        var page: String? = null
        appPreferencesStore.editData { preferences ->
            page = preferences[PROMO_PAGE_KEY]
            preferences.remove(PROMO_PAGE_KEY)
        }
        return page
    }

    private companion object {
        val PROMO_PAGE_KEY = stringPreferencesKey("promo_page")
    }
}