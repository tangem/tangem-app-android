package com.tangem.features.feed.model.news.list

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.chip.entity.ChipUM
import com.tangem.domain.news.model.NewsListConfig
import com.tangem.domain.news.usecase.GetNewsCategoriesUseCase
import com.tangem.domain.news.usecase.GetNewsListBatchFlowUseCase
import com.tangem.features.feed.components.news.list.DefaultNewsListComponent
import com.tangem.features.feed.model.news.list.calculator.NewsListStateManager
import com.tangem.features.feed.model.news.list.loader.NewsCategoriesLoader
import com.tangem.features.feed.model.news.list.statemanager.NewsListBatchFlowManager
import com.tangem.features.feed.ui.news.list.state.NewsListState
import com.tangem.features.feed.ui.news.list.state.NewsListUM
import com.tangem.utils.Provider
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class NewsListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getNewsCategoriesUseCase: GetNewsCategoriesUseCase,
    private val getNewsListBatchFlowUseCase: GetNewsListBatchFlowUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<DefaultNewsListComponent.Params>()
    private val selectedCategoryId = MutableStateFlow<Int?>(null)
    private val currentLanguage = SupportedLanguages.getCurrentSupportedLanguageCode()

    private val categoriesLoader by lazy {
        NewsCategoriesLoader(
            getNewsCategoriesUseCase = getNewsCategoriesUseCase,
            defaultAllNewsCategoryId = DEFAULT_ALL_NEWS_CATEGORIES_ID,
            onCategoryClick = ::onCategoryClick,
        )
    }

    private val batchFlowManager by lazy {
        NewsListBatchFlowManager(
            getNewsListBatchFlowUseCase = getNewsListBatchFlowUseCase,
            currentLanguage = Provider { currentLanguage },
            currentCategoryIds = Provider {
                selectedCategoryId.value?.takeIf { it > 0 }?.let { listOf(it) }.orEmpty()
            },
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    private val _state = MutableStateFlow(
        NewsListUM(
            selectedCategoryId = DEFAULT_ALL_NEWS_CATEGORIES_ID,
            filters = persistentListOf(),
            newsListState = NewsListState.Loading,
            listOfArticles = persistentListOf(),
            onArticleClick = { articleId ->
                params.onArticleClicked(
                    /* currentArticle */ articleId,
                    /* prefetchedArticles */ getCurrentFetchedArticlesIds(),
                    /* paginationConfig */ createNewsListConfig(),
                )
            },
            onBackClick = params.onBackClick,
        ),
    )
    val state = _state.asStateFlow()

    init {
        loadCategories()
        observeNewsList()
        batchFlowManager.reload()
    }

    private fun loadCategories() {
        modelScope.launch(dispatchers.default) {
            val filterChips = categoriesLoader.load()
            _state.update { currentState ->
                currentState.copy(filters = filterChips)
            }
        }
    }

    private fun observeNewsList() {
        modelScope.launch(dispatchers.default) {
            combine(
                batchFlowManager.uiItems,
                batchFlowManager.isInInitialLoadingErrorState,
                batchFlowManager.paginationStatus,
            ) { articles, isError, paginationStatus ->
                NewsListStateManager.calculateState(
                    articles = articles,
                    isError = isError,
                    paginationStatus = paginationStatus,
                    onRetryClick = {
                        loadCategories()
                        batchFlowManager.reload()
                    },
                    onLoadMore = { batchFlowManager.loadMore() },
                )
            }.collect { (listState, articles) ->
                _state.update { currentState ->
                    currentState.copy(
                        listOfArticles = articles,
                        newsListState = listState,
                    )
                }
            }
        }
    }

    private fun onCategoryClick(categoryId: Int) {
        val newCategoryId = if (state.value.selectedCategoryId == categoryId) {
            DEFAULT_ALL_NEWS_CATEGORIES_ID
        } else {
            categoryId
        }
        selectedCategoryId.value = newCategoryId
        _state.update { currentState ->
            currentState.copy(
                selectedCategoryId = newCategoryId,
                filters = updateFilterChips(newCategoryId),
            )
        }
        batchFlowManager.reload()
    }

    private fun updateFilterChips(categoryId: Int?): ImmutableList<ChipUM> {
        return state.value.filters.map { chip ->
            chip.copy(isSelected = chip.id == categoryId)
        }.toImmutableList()
    }

    private fun getCurrentFetchedArticlesIds(): List<Int> {
        return state.value.listOfArticles.map { it.id }
    }

    private fun createNewsListConfig(): NewsListConfig {
        return NewsListConfig(
            language = currentLanguage,
            snapshot = null,
            tokenIds = emptyList(),
            categoryIds = selectedCategoryId.value?.takeIf { it > 0 }?.let { listOf(it) }.orEmpty(),
        )
    }

    companion object {
        private const val DEFAULT_ALL_NEWS_CATEGORIES_ID = -1
    }
}