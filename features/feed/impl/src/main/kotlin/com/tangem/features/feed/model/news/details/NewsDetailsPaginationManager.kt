package com.tangem.features.feed.model.news.details

import com.tangem.domain.news.usecase.GetNewsListBatchFlowUseCase
import com.tangem.domain.news.usecase.ObserveNewsDetailsUseCase
import com.tangem.features.feed.model.news.list.statemanager.NewsListBatchFlowManager
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class NewsDetailsPaginationManager(
    private val observeNewsDetailsUseCase: ObserveNewsDetailsUseCase,
    private val currentLanguage: Provider<String>,
    getNewsListBatchFlowUseCase: GetNewsListBatchFlowUseCase,
    dispatchers: CoroutineDispatcherProvider,
    currentCategoryIds: Provider<List<Int>>,
    modelScope: CoroutineScope,
    prefetchedIds: Set<Int>,
) : NewsListBatchFlowManager(
    getNewsListBatchFlowUseCase = getNewsListBatchFlowUseCase,
    currentLanguage = currentLanguage,
    currentCategoryIds = currentCategoryIds,
    modelScope = modelScope,
    dispatchers = dispatchers,
) {

    private val _cachedPrefetchedIds = MutableStateFlow(prefetchedIds)
    val cachedPrefetchedIds = _cachedPrefetchedIds.asStateFlow()

    init {
        reload()
    }

    fun start() {
        modelScope.launch(dispatchers.default) {
            combine(
                rawArticlesFlow,
                observeNewsDetailsUseCase(),
                _cachedPrefetchedIds,
            ) { articles, cachedArticles, prefetched ->
                calculateIdsToFetch(
                    visibleIds = articles.map { it.id }.toSet(),
                    cachedIds = cachedArticles.keys,
                    alreadyPrefetched = prefetched,
                )
            }
                .distinctUntilChanged()
                .collect { newIds ->
                    if (newIds.isNotEmpty()) {
                        observeNewsDetailsUseCase.prefetch(
                            newsIds = newIds,
                            language = currentLanguage(),
                        )
                        _cachedPrefetchedIds.update { it + newIds }
                    }
                }
        }
    }

    private fun calculateIdsToFetch(visibleIds: Set<Int>, cachedIds: Set<Int>, alreadyPrefetched: Set<Int>): Set<Int> {
        return visibleIds - cachedIds - alreadyPrefetched
    }

    fun checkAndLoadMoreIfNeeded(
        currentIndex: Int,
        totalArticlesCount: Int,
        preloadThreshold: Int = PRELOAD_THRESHOLD,
    ) {
        if (currentIndex >= totalArticlesCount - preloadThreshold) {
            loadMoreIfPossible()
        }
    }

    private fun loadMoreIfPossible() {
        modelScope.launch(dispatchers.default) {
            when (val status = paginationStatus.value) {
                is PaginationStatus.InitialLoading,
                is PaginationStatus.NextBatchLoading,
                is PaginationStatus.EndOfPagination,
                -> {
                    return@launch
                }

                is PaginationStatus.InitialLoadingError -> {
                    reload()
                    return@launch
                }

                is PaginationStatus.Paginating -> {
                    val lastResult = status.lastResult
                    val canLoadMore = lastResult is BatchFetchResult.Success && !lastResult.last

                    if (canLoadMore) {
                        loadMore()
                    }
                }

                is PaginationStatus.None -> {
                    reload()
                }
            }
        }
    }

    companion object {
        private const val PRELOAD_THRESHOLD = 5
    }
}