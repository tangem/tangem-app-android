package com.tangem.data.news.store.details

import com.tangem.domain.models.news.DetailedArticle
import kotlinx.coroutines.flow.Flow

internal interface NewsDetailsStore {

    fun getAll(): Flow<List<DetailedArticle>>

    suspend fun getSyncOrNull(id: Int): DetailedArticle?

    suspend fun store(id: Int, article: DetailedArticle)

    suspend fun store(articles: Map<Int, DetailedArticle>)

    suspend fun clear()
}