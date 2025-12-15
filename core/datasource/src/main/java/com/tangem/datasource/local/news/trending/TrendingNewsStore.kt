package com.tangem.datasource.local.news.trending

import com.tangem.domain.models.news.ShortArticle
import kotlinx.coroutines.flow.Flow

interface TrendingNewsStore {

    fun get(key: String): Flow<List<ShortArticle>>

    suspend fun getSyncOrNull(key: String): List<ShortArticle>?

    suspend fun store(key: String, value: List<ShortArticle>)

    suspend fun clear()
}