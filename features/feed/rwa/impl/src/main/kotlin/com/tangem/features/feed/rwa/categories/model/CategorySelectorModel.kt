package com.tangem.features.feed.rwa.categories.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.GetCoinCategoriesUseCase
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.categories.RwaDefaultCategory
import com.tangem.features.feed.rwa.categories.model.transformer.CategorySelectorTransformer
import com.tangem.features.feed.rwa.model.UserCountryResolver
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tracks the draft selection separately from the applied one, so nothing reaches the tab until Apply.
 *
 * Two categories are not plain filters. The `featured-assets` one is excluded: it populates the tab's
 * Featured rail. The `real-world-assets` umbrella leads the list titled "All": it is the tab's default
 * scope, so picking it without a sector applies the empty [CategorySelection].
 */
@ModelScoped
internal class CategorySelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase,
    private val userCountryResolver: UserCountryResolver,
) : Model() {

    private val params = paramsContainer.require<CategorySelectorRoute>()

    private var categories: List<CoinCategory> = emptyList()
    private var country: String? = null
    private var draftSelection = params.categorySelection

    val uiState: StateFlow<CategorySelectorUM>
        field = MutableStateFlow<CategorySelectorUM>(CategorySelectorUM.Loading)

    init {
        loadCategories()
    }

    private fun loadCategories() {
        uiState.update { CategorySelectorUM.Loading }

        modelScope.launch {
            country = userCountryResolver.resolve()

            getCoinCategoriesUseCase(country = country).fold(
                ifLeft = {
                    uiState.update { CategorySelectorUM.Error(onRetry = ::loadCategories) }
                },
                ifRight = { loaded ->
                    categories = loaded
                        .filterNot { it.code == ReservedCategoryCodes.FEATURED_ASSETS }
                        // the umbrella arrives invisible, yet it is the row that clears the filter —
                        // dropping it would leave the user no way back to "All"
                        .filter { it.isVisible || it.code == ReservedCategoryCodes.REAL_WORLD_ASSETS }
                        .sortedBy { it.displayOrder }

                    updateState(selection = params.categorySelection, isEdited = false)
                },
            )
        }
    }

    /**
     * Both sections speak the same language: a tap publishes the whole [CategorySelection] it wants
     * applied, rather than the one level it changed. The umbrella row therefore reaches here as the
     * empty selection, and a sector tap arrives with its category already attached.
     */
    private fun onSelectionChange(selection: CategorySelection) {
        draftSelection = selection

        updateState(selection = selection, isEdited = selection != params.categorySelection)
    }

    /**
     * Reset goes back to the scope the region opens on, not to the scope the screen was opened with — a
     * user in a restricted region lands on the regional default, everyone else on the unfiltered list.
     */
    private fun onReset() {
        draftSelection = RwaDefaultCategory.resolve(
            categories = categories,
            country = country,
            hasUserSelection = false,
        ) ?: CategorySelection()

        updateState(selection = draftSelection, isEdited = draftSelection != params.categorySelection)
    }

    private fun onApply() {
        params.callback.onCategorySelect(draftSelection)
        router.pop()
    }

    private fun updateState(selection: CategorySelection, isEdited: Boolean) {
        uiState.update(
            transformer = CategorySelectorTransformer(
                categoryList = categories,
                categorySelection = selection,
                isEdited = isEdited,
                onReset = ::onReset,
                onApply = ::onApply,
                onSelectionChange = ::onSelectionChange,
            ),
        )
    }
}