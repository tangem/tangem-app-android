package com.tangem.features.news.details.impl

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.news.details.api.NewsDetailsComponent
import com.tangem.features.news.details.impl.ui.NewsDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.indexOfFirstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ModelScoped
internal class NewsDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    private val mockedArticles = MockArticlesFactory.createMockArticles()

    private val params = paramsContainer.require<NewsDetailsComponent.Params>()

    private val _uiState = MutableStateFlow(
        NewsDetailsUM(
            /* [REDACTED_TODO_COMMENT] */
            articles = mockedArticles,
            selectedArticleIndex = mockedArticles.indexOfFirstOrNull { it.id == params.selectedArticleId } ?: 0,
            onShareClick = { /* [REDACTED_TODO_COMMENT] */ },
            onLikeClick = { /* [REDACTED_TODO_COMMENT] */ },
        ),
    )
    val uiState: StateFlow<NewsDetailsUM> = _uiState.asStateFlow()

    fun onBackClick() {
        router.pop()
    }
}