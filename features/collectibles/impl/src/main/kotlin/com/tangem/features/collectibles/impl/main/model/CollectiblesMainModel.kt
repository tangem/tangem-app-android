package com.tangem.features.collectibles.impl.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesActionsConverter
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesBalanceConverter
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesContentConverter
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesMenuConverter
import com.tangem.features.collectibles.impl.main.model.converter.CollectiblesTabsConverter
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 *
 * The catalogue is local (see [CollectiblesMainStubData]), so the screen never leaves the loaded state on its
 * own — everything the user can change is category, query and menu visibility.
 */
@ModelScoped
internal class CollectiblesMainModel @Inject constructor(
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val balanceConverter = CollectiblesBalanceConverter()
    private val actionsConverter = CollectiblesActionsConverter(
        onAddFundsClick = ::onAddFundsClick,
        onWithdrawalClick = ::onWithdrawalClick,
    )
    private val menuConverter = CollectiblesMenuConverter(
        onMenuClick = ::onMenuClick,
        onDismiss = ::onMenuDismiss,
        onActivityLogClick = ::onActivityLogClick,
        onDeliveriesClick = ::onDeliveriesClick,
        onHowItWorksClick = ::onHowItWorksClick,
        onGetAssistanceClick = ::onGetAssistanceClick,
    )
    private val tabsConverter = CollectiblesTabsConverter(onCategoryClick = ::onCategoryClick)

    private val packs = MutableStateFlow<List<CollectiblesPack>>(value = emptyList())

    val uiState: StateFlow<CollectiblesMainUM>
        field = MutableStateFlow(buildInitialState())

    init {
        loadCards()
    }

    private fun buildInitialState(): CollectiblesMainUM {
        return CollectiblesMainUM(
            title = stringReference("Packs account"),
            balance = balanceConverter.convert(CollectiblesMainStubData.BALANCE),
            actions = actionsConverter.convert(Unit),
            collection = CollectiblesMainUM.CollectionUM(
                subtitle = stringReference("My collection"),
                title = stringReference("${CollectiblesMainStubData.COLLECTION_SIZE} cards"),
                onClick = ::onCollectionClick,
            ),
            tabs = tabsConverter.convert(CollectiblesMainStubData.CATEGORIES),
            content = CollectiblesMainUM.ContentUM.Loading,
            search = TangemSearch.State(
                placeholderText = stringReference("Search"),
                query = "",
                onQueryChange = ::onQueryChange,
                isActive = false,
                onActiveChange = ::onSearchActiveChange,
                onClearClick = ::onClearQueryClick,
                onCloseClick = ::onCloseSearchClick,
            ),
            menu = menuConverter.convert(Unit),
            onBackClick = ::onBackClick,
        )
    }

    private fun loadCards() {
        // TODO([REDACTED_TASK_KEY]): the catalogue is local, so there is nothing to request. With a backend this keeps the
        //  loading content until the service answers and lands on the packs or on ContentUM.Error(::loadCards).
        packs.value = CollectiblesMainStubData.PACKS
        uiState.update { state ->
            state.copy(content = contentUM(categoryId = state.selectedCategoryId, query = state.search.query))
        }
    }

    private fun contentUM(categoryId: String, query: String): CollectiblesMainUM.ContentUM {
        return CollectiblesContentConverter(
            categoryId = categoryId,
            query = query,
            onPackClick = ::onPackClick,
        ).convert(packs.value)
    }

    private fun onCategoryClick(categoryId: String) {
        uiState.update { state ->
            state.copy(
                tabs = state.tabs.select(categoryId),
                content = contentUM(categoryId = categoryId, query = state.search.query),
            )
        }
    }

    private fun onQueryChange(query: String) {
        uiState.update { state ->
            state.copy(
                search = state.search.copy(query = query),
                content = contentUM(categoryId = state.selectedCategoryId, query = query),
            )
        }
    }

    private fun onSearchActiveChange(isActive: Boolean) {
        uiState.update { state -> state.copy(search = state.search.copy(isActive = isActive)) }
    }

    private fun onClearQueryClick() = onQueryChange(query = "")

    private fun onCloseSearchClick() {
        uiState.update { state ->
            state.copy(
                search = state.search.copy(query = "", isActive = false),
                content = contentUM(categoryId = state.selectedCategoryId, query = ""),
            )
        }
    }

    private fun onMenuClick() {
        uiState.update { state -> state.copy(menu = state.menu.copy(isShown = true)) }
    }

    private fun onMenuDismiss() {
        uiState.update { state -> state.copy(menu = state.menu.copy(isShown = false)) }
    }

    private fun onHowItWorksClick() {
        onMenuDismiss()
        router.push(CollectiblesRoute.Stories)
    }

    private fun onBackClick() {
        router.pop()
    }

    @Suppress("EmptyFunctionBlock")
    private fun onAddFundsClick() {
        // TODO([REDACTED_TASK_KEY]): top-up flow is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onWithdrawalClick() {
        // TODO([REDACTED_TASK_KEY]): withdrawal flow is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onCollectionClick() {
        // TODO([REDACTED_TASK_KEY]): the collection screen is not built yet.
    }

    @Suppress("EmptyFunctionBlock", "UnusedParameter")
    private fun onPackClick(packId: String) {
        // TODO([REDACTED_TASK_KEY]): the pack details screen is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onActivityLogClick() {
        // TODO([REDACTED_TASK_KEY]): the activity log screen is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onDeliveriesClick() {
        // TODO([REDACTED_TASK_KEY]): the deliveries screen is not built yet.
    }

    @Suppress("EmptyFunctionBlock")
    private fun onGetAssistanceClick() {
        // TODO([REDACTED_TASK_KEY]): support entry point is not wired yet.
    }

    private companion object {

        val CollectiblesMainUM.selectedCategoryId: String
            get() = tabs.filterIsInstance<TangemTabItemUM.Content>()
                .firstOrNull(TangemTabItemUM.Content::isSelected)
                ?.id
                ?: CollectiblesMainStubData.ALL_CATEGORY_ID

        fun ImmutableList<TangemTabItemUM>.select(categoryId: String): ImmutableList<TangemTabItemUM> {
            return map { tab ->
                if (tab is TangemTabItemUM.Content) tab.copy(isSelected = tab.id == categoryId) else tab
            }.toImmutableList()
        }
    }
}