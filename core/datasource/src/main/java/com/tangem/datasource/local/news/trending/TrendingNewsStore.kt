package com.tangem.datasource.local.news.trending

import com.tangem.domain.models.news.TrendingNews
import kotlinx.coroutines.flow.Flow

interface TrendingNewsStore {

    fun get(key: String): Flow<TrendingNews>

    suspend fun getSyncOrNull(key: String): TrendingNews?

    suspend fun store(key: String, value: TrendingNews)

    suspend fun clear()
}