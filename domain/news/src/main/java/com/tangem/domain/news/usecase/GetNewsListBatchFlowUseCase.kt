package com.tangem.domain.news.usecase

import com.tangem.domain.news.model.NewsListBatchFlow
import com.tangem.domain.news.model.NewsListBatchingContext
import com.tangem.domain.news.repository.NewsRepository

/**
 * Provides a pagination-aware flow of short articles.
 *
 * Mirrors behaviour of other BatchFlow use cases (e.g., manage tokens) and can be used with pagers.
 */
class GetNewsListBatchFlowUseCase(
    private val repository: NewsRepository,
) {

    /**
     * Creates a [NewsListBatchFlow] for the given batching context and page size.
     */
    operator fun invoke(context: NewsListBatchingContext, batchSize: Int = DEFAULT_BATCH_SIZE): NewsListBatchFlow {
        return repository.getNewsListBatchFlow(context, batchSize)
    }

    private companion object {
        private const val DEFAULT_BATCH_SIZE = 20
    }
}