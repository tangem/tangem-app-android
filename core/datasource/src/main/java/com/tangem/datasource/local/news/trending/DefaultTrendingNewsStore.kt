package com.tangem.datasource.local.news.trending

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.models.news.ShortArticle
import kotlinx.coroutines.flow.Flow

internal class DefaultTrendingNewsStore(
    dataStore: StringKeyDataStore<List<ShortArticle>>,
) : TrendingNewsStore, StringKeyDataStoreDecorator<String, List<ShortArticle>>(dataStore) {

    override fun provideStringKey(key: String): String = key

    override fun get(key: String): Flow<List<ShortArticle>> = super.get(key)

    override suspend fun getSyncOrNull(key: String): List<ShortArticle>? = super.getSyncOrNull(key)

    override suspend fun store(key: String, value: List<ShortArticle>) {
        super.store(key, value)
    }

    override suspend fun clear() {
        super.clear()
    }
}