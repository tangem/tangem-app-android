package com.tangem.features.feed.model.news.details

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.news.usecase.ObserveNewsDetailsUseCase
import com.tangem.features.feed.components.news.details.DefaultNewsDetailsComponent
import com.tangem.features.feed.model.news.details.converter.NewsDetailsConverter
import com.tangem.features.feed.ui.news.details.state.NewsDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.indexOfFirstOrNull
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class NewsDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val observeNewsDetailsUseCase: ObserveNewsDetailsUseCase,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<DefaultNewsDetailsComponent.Params>()

    private val currentLanguage = Locale.getDefault().language

    private val converter = NewsDetailsConverter(
        onSourceClick = { url ->
            urlOpener.openUrl(url)
        },
    )

    private val _state = MutableStateFlow(
        NewsDetailsUM(
            articles = persistentListOf(),
            selectedArticleIndex = 0,
            onShareClick = {},
            onLikeClick = { /* [REDACTED_TODO_COMMENT] */ },
            onBackClick = params.onBackClicked,
            onArticleIndexChanged = ::onArticleIndexChanged,
        ),
    )

    val state: StateFlow<NewsDetailsUM> = _state.asStateFlow()

    init {
        if (params.preselectedArticlesId.isNotEmpty()) {
            handlePreselectedArticles()
        } else {
            // TODO handle pagination [REDACTED_TASK_KEY]
        }
    }

    private fun onArticleIndexChanged(newIndex: Int) {
        val currentArticle = state.value.articles.getOrNull(newIndex)
        _state.update { currentState ->
            currentState.copy(
                selectedArticleIndex = newIndex,
                onShareClick = {
                    currentArticle?.let {
                        shareManager.shareText(it.newsUrl)
                    }
                },
            )
        }
    }

    private fun handlePreselectedArticles() {
        modelScope.launch {
            observeNewsDetailsUseCase.prefetch(
                newsIds = params.preselectedArticlesId,
                language = currentLanguage,
            )

            observeNewsDetailsUseCase
                .invoke()
                .map { articlesMap ->
                    params.preselectedArticlesId.mapNotNull { articleId ->
                        articlesMap[articleId]?.let { detailedArticle ->
                            converter.convert(detailedArticle)
                        }
                    }
                }
                .map { articles ->
                    articles.toImmutableList()
                }
                .onEach { articles ->
                    val selectedIndex = articles.indexOfFirstOrNull { it.id == params.articleId } ?: 0
                    val currentArticle = articles.getOrNull(selectedIndex)
                    _state.update { newsDetailsUM ->
                        newsDetailsUM.copy(
                            articles = articles,
                            selectedArticleIndex = selectedIndex,
                            onShareClick = {
                                currentArticle?.let {
                                    shareManager.shareText(it.newsUrl)
                                }
                            },
                        )
                    }
                }
                .launchIn(modelScope)
        }
    }
}