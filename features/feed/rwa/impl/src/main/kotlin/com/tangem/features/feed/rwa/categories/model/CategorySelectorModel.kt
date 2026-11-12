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
 * A tap applies and closes, unless it picks a category with a sector choice: that one becomes a draft
 * the Sectors rail refines, and only Apply hands the category + sector pair to the tab. With "All" as a
 * category row there is nothing for a Reset to do.
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
    private var draftSelection = params.categorySelection

    val uiState: StateFlow<CategorySelectorUM>
        field = MutableStateFlow<CategorySelectorUM>(CategorySelectorUM.Loading)

    init {
        loadCategories()
    }

    private fun loadCategories() {
        uiState.update { CategorySelectorUM.Loading }

        modelScope.launch {
            getCoinCategoriesUseCase(country = userCountryResolver.resolve()).fold(
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

                    updateState()
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
        val category = categories.firstOrNull { it.id == selection.category?.id }

        if (category?.hasSectorChoice == true) {
            draftSelection = selection
            updateState()
        } else {
            applyAndClose(selection)
        }
    }

    private fun onApply() {
        applyAndClose(draftSelection)
    }

    /** An unchanged filter is not a pick: the tab would reload for nothing and report a `Select Category`. */
    private fun applyAndClose(selection: CategorySelection) {
        if (selection != params.categorySelection) params.callback.onCategorySelect(selection)
        router.pop()
    }

    private fun updateState() {
        uiState.update(
            transformer = CategorySelectorTransformer(
                categoryList = categories,
                categorySelection = draftSelection,
                isApplyVisible = draftSelection != params.categorySelection,
                onApply = ::onApply,
                onSelectionChange = ::onSelectionChange,
            ),
        )
    }
}