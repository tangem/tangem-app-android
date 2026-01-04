package com.tangem.features.feed.model.news.list.statemanager

import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.news.model.NewsListBatchingContext
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.domain.news.usecase.GetNewsListBatchFlowUseCase
import com.tangem.features.feed.model.converter.ShortArticleToArticleConfigUMConverter
import com.tangem.features.feed.model.converter.distinctBatchesContent
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal open class NewsListBatchFlowManager(
    getNewsListBatchFlowUseCase: GetNewsListBatchFlowUseCase,
    private val currentLanguage: Provider<String>,
    private val currentCategoryIds: Provider<List<Int>>,
    protected val modelScope: CoroutineScope,
    protected val dispatchers: CoroutineDispatcherProvider,
) {
    private val actionsFlow = MutableSharedFlow<BatchAction<Int, NewsListConfig, Nothing>>()
    private val converter by lazy {
        ShortArticleToArticleConfigUMConverter(isTrending = Provider { false })
    }

    private val batchFlow = getNewsListBatchFlowUseCase(
        context = NewsListBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
        batchSize = DEFAULT_BATCH_SIZE,
    )

    private val resultBatches = MutableStateFlow<List<Batch<Int, List<ArticleConfigUM>>>>(emptyList())

    val rawArticlesFlow: StateFlow<ImmutableList<ShortArticle>> = batchFlow.state
        .map { batchListState ->
            batchListState.data
                .flatMap { batch -> batch.data }
                .toImmutableList()
        }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = persistentListOf(),
        )

    val uiItems: StateFlow<ImmutableList<ArticleConfigUM>>
        get() = batchFlow.state
            .map { batchListState ->
                batchListState.data
                    .flatMap { batch -> batch.data }
                    .let { articles -> converter.convert(articles) }
            }
            .distinctUntilChanged()
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = persistentListOf(),
            )

    val isInInitialLoadingErrorState = batchFlow.state
        .map { it.status is PaginationStatus.InitialLoadingError }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val paginationStatus: StateFlow<PaginationStatus<List<ShortArticle>>> = batchFlow.state
        .map { it.status }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = PaginationStatus.InitialLoading,
        )

    init {
        batchFlow.state
            .map { it.data }
            .distinctBatchesContent()
            .onEach { batches ->
                resultBatches.value = batches.map { batch ->
                    Batch(
                        key = batch.key,
                        data = converter.convert(batch.data),
                    )
                }
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    fun reload() {
        modelScope.launch(dispatchers.default) {
            resultBatches.value = emptyList()
            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = createNewsListConfig(),
                ),
            )
        }
    }

    fun loadMore() {
        modelScope.launch(dispatchers.default) {
            actionsFlow.emit(BatchAction.LoadMore())
        }
    }

    private fun createNewsListConfig(): NewsListConfig {
        return NewsListConfig(
            language = currentLanguage(),
            snapshot = null,
            tokenIds = emptyList(),
            categoryIds = currentCategoryIds(),
        )
    }

    private companion object {
        private const val DEFAULT_BATCH_SIZE = 20
    }
}