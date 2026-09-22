package com.tangem.features.collectibles.impl.discovery.ui.preview

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.discovery.CollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.discovery.ui.collectiblesDiscoveryList
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal object CollectiblesDiscoveryPreviewData {

    val pack = CollectiblesDiscoveryUM.PackUM(
        id = "legendary_1",
        imageUrl = "",
        title = stringReference("Legendary 1"),
        price = stringReference("$50"),
        badge = stringReference("Buyback 85%"),
        onClick = {},
    )

    val default = CollectiblesDiscoveryUM(
        tabs = CollectiblesDiscoveryUM.TabsUM.Content(
            tabs = persistentListOf(
                tab(id = "all", label = "All cards", counter = "32", isSelected = true),
                tab(id = "sports", label = "Sports", counter = "8"),
                tab(id = "pokemon", label = "Pokemon", counter = "12"),
                tab(id = "something_else", label = "Something else", counter = "6"),
                tab(id = "anime", label = "Anime", counter = "6"),
            ),
        ),
        content = content(
            pack,
            pack.copy(id = "water_1", title = stringReference("Water 1")),
            pack.copy(id = "elite_1", title = stringReference("Elite 1")),
            pack.copy(id = "legendary_2", title = stringReference("Legendary 2")),
        ),
        search = searchState(query = "", isActive = false),
        hasSearchableCatalogue = true,
    )

    // The catalogue's size comes with the categories, so both states that have none also have no search.
    val loading = default.copy(
        tabs = CollectiblesDiscoveryUM.TabsUM.Loading,
        content = CollectiblesDiscoveryUM.ContentUM.Loading,
        hasSearchableCatalogue = false,
    )

    val listError = default.copy(
        tabs = CollectiblesDiscoveryUM.TabsUM.Hidden,
        content = CollectiblesDiscoveryUM.ContentUM.Error(
            message = stringReference("Failed to load cards list.\nTap to reload"),
            onReloadClick = {},
        ),
        hasSearchableCatalogue = false,
    )

    val searchActive = default.copy(
        tabs = CollectiblesDiscoveryUM.TabsUM.Hidden,
        content = content(
            pack,
            pack.copy(id = "water_1", title = stringReference("Water 1")),
        ),
        search = searchState(query = "Serhii", isActive = true),
    )

    val searchZero = searchActive.copy(
        content = CollectiblesDiscoveryUM.ContentUM.Empty(
            message = stringReference("No results. Try to change your wording\nor explore from the list"),
        ),
    )

    val searchError = searchActive.copy(
        content = CollectiblesDiscoveryUM.ContentUM.Error(
            message = stringReference("Failed to load cards list.\nTap to reload"),
            onReloadClick = {},
        ),
    )

    val loadingNextPage = default.copy(
        content = (default.content as CollectiblesDiscoveryUM.ContentUM.Content).copy(isLoadingNextPage = true),
    )

    private fun content(vararg packs: CollectiblesDiscoveryUM.PackUM) = CollectiblesDiscoveryUM.ContentUM.Content(
        rows = packs.toList()
            .chunked(size = 2) { chunk ->
                CollectiblesDiscoveryUM.PackRowUM(id = chunk.first().id, packs = chunk.toImmutableList())
            }
            .toImmutableList(),
        isLoadingNextPage = false,
        isReloading = false,
    )

    private fun tab(id: String, label: String, counter: String, isSelected: Boolean = false) = TangemTabItemUM.Content(
        id = id,
        label = stringReference(label),
        counter = stringReference(counter),
        isSelected = isSelected,
        onClick = {},
    )

    private fun searchState(query: String, isActive: Boolean) = TangemSearch.State(
        placeholderText = stringReference("Search"),
        query = query,
        onQueryChange = {},
        isActive = isActive,
        onActiveChange = {},
        onClearClick = {},
        onCloseClick = {},
    )
}

/** Model-less stand-in so the main screen previews can embed the discovery items. */
internal class CollectiblesDiscoveryPreviewComponent(
    um: CollectiblesDiscoveryUM,
) : CollectiblesDiscoveryComponent {

    override val uiState: StateFlow<CollectiblesDiscoveryUM> = MutableStateFlow(um)

    override val tabScrollState: ScrollState = ScrollState(initial = 0)

    override fun onLoadMore() { /* no-op */ }

    override fun onRefresh() { /* no-op */ }

    override fun LazyListScope.content(uiState: CollectiblesDiscoveryUM, modifier: Modifier) {
        collectiblesDiscoveryList(state = uiState, tabScrollState = tabScrollState)
    }
}