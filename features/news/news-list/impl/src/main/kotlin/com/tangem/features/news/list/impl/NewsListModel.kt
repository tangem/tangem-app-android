package com.tangem.features.news.list.impl

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.chip.entity.ChipUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.news.usecase.GetNewsCategoriesUseCase
import com.tangem.features.news.list.impl.ui.NewsListUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class NewsListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getNewsCategoriesUseCase: GetNewsCategoriesUseCase,
    private val router: Router,
) : Model() {

    val uiState: StateFlow<NewsListUM>
        field = MutableStateFlow(
            NewsListUM(
                selectedCategoryId = 0,
                filters = persistentListOf(),
                articles = persistentListOf(),
                onArticleClick = ::onArticleClick,
            ),
        )

    init {
        modelScope.launch(dispatchers.default) {
            val filterChips = getNewsCategoriesUseCase
                .invoke()
                .map { articleCategory ->
                    ChipUM(
                        id = articleCategory.id,
                        text = TextReference.Str(articleCategory.name),
                        isSelected = false,
                        onClick = {
                            onCategoryClick(articleCategory.id)
                        },
                    )
                }
                .toImmutableList()
            uiState.update { currentState ->
                currentState.copy(filters = filterChips)
            }
        }
    }

    fun onBackClick() {
        router.pop()
    }

    private fun onArticleClick(articleId: Int) {
        // TODO [REDACTED_TASK_KEY]
        articleId
    }

    private fun onCategoryClick(categoryId: Int) {
        uiState.update { currentState ->
            currentState.copy(
                selectedCategoryId = categoryId,
                filters = updateFilterChips(categoryId),
            )
        }
    }

    private fun updateFilterChips(categoryId: Int): ImmutableList<ChipUM> {
        return uiState.value.filters.map { chip ->
            chip.copy(isSelected = chip.id == categoryId)
        }.toImmutableList()
    }
}