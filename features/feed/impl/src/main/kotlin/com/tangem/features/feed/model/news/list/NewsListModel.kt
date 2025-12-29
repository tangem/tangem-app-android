package com.tangem.features.feed.model.news.list

import com.tangem.common.ui.R
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.chip.entity.ChipUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.news.usecase.GetNewsCategoriesUseCase
import com.tangem.domain.news.usecase.GetNewsListBatchFlowUseCase
import com.tangem.features.feed.components.news.list.DefaultNewsListComponent
import com.tangem.features.feed.model.news.list.statemanager.NewsListBatchFlowManager
import com.tangem.features.feed.ui.news.list.state.NewsListState
import com.tangem.features.feed.ui.news.list.state.NewsListUM
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
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
            val allCategoriesChip = ChipUM(
                id = DEFAULT_ALL_NEWS_CATEGORIES_ID,
                text = TextReference.Res(R.string.news_all_news),
                isSelected = true,
                onClick = { onCategoryClick(DEFAULT_ALL_NEWS_CATEGORIES_ID) },
            )
            val filterChips = getNewsCategoriesUseCase
                .invoke()
                .fold(
                    ifLeft = {
                        persistentListOf()
                    },
                    ifRight = { categories ->
                        (listOf(allCategoriesChip) + categories.map { articleCategory ->
                            ChipUM(
                                id = articleCategory.id,
                                text = TextReference.Str(articleCategory.name),
                                isSelected = false,
                                onClick = {
                                    onCategoryClick(articleCategory.id)
                                },
                            )
                        }).toPersistentList()
                    },
                )
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
                when {
                    isError -> NewsListState.LoadingError(
                        onRetryClicked = {
                            loadCategories()
                            batchFlowManager.reload()
                        },
                    ) to persistentListOf()
                    paginationStatus is PaginationStatus.InitialLoading && articles.isEmpty() -> {
                        NewsListState.Loading to persistentListOf()
                    }
                    articles.isEmpty() -> {
                        NewsListState.Loading to persistentListOf()
                    }
                    else -> {
                        NewsListState.Content(loadMore = { batchFlowManager.loadMore() }) to articles
                    }
                }
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

    companion object {
        private const val DEFAULT_ALL_NEWS_CATEGORIES_ID = -1
    }
}