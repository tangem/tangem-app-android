package com.tangem.domain.news.usecase

import com.tangem.domain.models.news.TrendingNews
import com.tangem.domain.news.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Exposes trending news as a cached flow and provides helpers to refresh or mark items as viewed.
 *
 * Keep a single instance per process (provided via DI) and collect the flow to render trending rows.
 */
class ManageTrendingNewsUseCase(private val repository: NewsRepository) {

    /**
     * Observes the current cached list of trending articles (max 10 items) or error.
     */
    fun observeTrendingNews(): Flow<TrendingNews> {
        return repository
            .observeTrendingNews()
            .distinctUntilChanged()
    }
}