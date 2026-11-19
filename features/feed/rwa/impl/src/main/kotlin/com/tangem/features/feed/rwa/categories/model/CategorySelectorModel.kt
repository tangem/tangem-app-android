package com.tangem.features.feed.rwa.categories.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.GetCoinCategoriesUseCase
import com.tangem.features.feed.FeedFeatureToggles
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.CategorySelectorRoute
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.categories.model.transformer.CategorySelectorTransformer
import com.tangem.features.feed.rwa.categories.model.transformer.LegacyCategorySelectorTransformer
import com.tangem.features.feed.rwa.model.UserCountryResolver
import com.tangem.features.feed.rwa.categories.ui.state.CategorySelectorUM
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Every pick — a category row or a sector chip — reaches the tab at once and closes the screen.
 * "All" (the umbrella row) is the reset. Without the redesign toggle the pre-redesign flow runs instead:
 * a category with a sector choice becomes a draft the Sectors rail refines, and only Apply hands it over.
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
    feedFeatureToggles: FeedFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<CategorySelectorRoute>()

    /** Read once, so an open screen never switches behaviour under the user. */
    private val isRedesignEnabled = feedFeatureToggles.isCategoryFiltersRedesignEnabled

    private var categories: List<CoinCategory> = emptyList()
    private var appliedSelection = params.categorySelection

    @RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
    private var draftSelection = appliedSelection

    val uiState: StateFlow<CategorySelectorUM>
        field = MutableStateFlow<CategorySelectorUM>(CategorySelectorUM.Loading)

    init {
        // a discover deeplink re-filters the tab while this screen stays open: show what the list now uses
        params.callback.appliedSelection
            .filter { it != appliedSelection }
            .onEach { selection ->
                appliedSelection = selection
                draftSelection = selection
                if (categories.isNotEmpty()) updateState()
            }
            .launchIn(modelScope)

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
     * Rows and chips speak the same language: a tap publishes the whole [CategorySelection] it wants
     * applied, rather than the one level it changed. The umbrella row therefore reaches here as the
     * empty selection, and a sector chip arrives with its category already attached.
     *
     * An unchanged pick is not one: the tab would reload for nothing and report a `Select Category`.
     */
    private fun onSelectionChange(selection: CategorySelection) {
        if (!isRedesignEnabled) {
            onLegacySelectionChange(selection)
            return
        }

        if (selection != appliedSelection) {
            // set before the callback, so its echo through appliedSelection is filtered out in init
            appliedSelection = selection
            params.callback.onCategorySelect(selection)
        }

        router.pop()
    }

    @RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
    private fun onLegacySelectionChange(selection: CategorySelection) {
        val category = categories.firstOrNull { it.id == selection.category?.id }

        if (category?.hasSectorChoice == true) {
            draftSelection = selection
            updateState()
        } else {
            applyAndClose(selection)
        }
    }

    @RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
    private fun onApply() {
        applyAndClose(draftSelection)
    }

    /** An unchanged filter is not a pick: the tab would reload for nothing and report a `Select Category`. */
    @RemoveWithToggle("AND_19514_CATEGORY_FILTERS_REDESIGN_ENABLED")
    private fun applyAndClose(selection: CategorySelection) {
        if (selection != appliedSelection) params.callback.onCategorySelect(selection)
        router.pop()
    }

    private fun updateState() {
        val transformer = if (isRedesignEnabled) {
            CategorySelectorTransformer(
                categoryList = categories,
                categorySelection = appliedSelection,
                onSelectionChange = ::onSelectionChange,
            )
        } else {
            LegacyCategorySelectorTransformer(
                categoryList = categories,
                categorySelection = draftSelection,
                isApplyVisible = draftSelection != appliedSelection,
                onApply = ::onApply,
                onSelectionChange = ::onSelectionChange,
            )
        }
        uiState.update(transformer = transformer)
    }
}