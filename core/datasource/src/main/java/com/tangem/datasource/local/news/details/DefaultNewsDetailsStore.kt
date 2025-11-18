package com.tangem.datasource.local.news.details

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.models.news.DetailedArticle
import kotlinx.coroutines.flow.Flow

internal class DefaultNewsDetailsStore(
    dataStore: StringKeyDataStore<DetailedArticle>,
) : NewsDetailsStore, StringKeyDataStoreDecorator<Int, DetailedArticle>(dataStore) {

    override fun provideStringKey(key: Int): String = key.toString()

    override fun getAll(): Flow<List<DetailedArticle>> = super.getAll()

    override suspend fun getSyncOrNull(id: Int): DetailedArticle? = super.getSyncOrNull(id)

    override suspend fun store(id: Int, article: DetailedArticle) {
        super.store(id, article)
    }

    override suspend fun store(articles: Map<Int, DetailedArticle>) {
        super.store(articles)
    }

    override suspend fun clear() {
        super.clear()
    }
}