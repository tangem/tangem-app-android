package com.tangem.features.collectibles.impl.inventory.ui.preview

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM.ContentUM
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM.ItemUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal object CollectiblesInventoryPreviewData {

    val info = ItemUM.Info(text = resourceReference(R.string.collectibles_inventory_buyback_description))

    val expiredCard = createCard(
        id = "expired",
        timeLeft = resourceReference(R.string.collectibles_inventory_timer_expired),
    )

    val underMinuteCard = createCard(
        id = "under_minute",
        timeLeft = resourceReference(R.string.collectibles_inventory_timer_under_minute),
    )

    val minutesCard = createCard(
        id = "minutes",
        timeLeft = resourceReference(R.string.collectibles_inventory_timer_minutes, wrappedList(45)),
    )

    val hoursCard = createCard(
        id = "hours",
        timeLeft = resourceReference(R.string.collectibles_inventory_timer_hours, wrappedList(48, 24)),
    )

    val collectionCard = createCard(id = "collection", timeLeft = null)

    val buyback = createState(
        isBuybackSelected = true,
        content = ContentUM.Items(
            items = persistentListOf(info, expiredCard, underMinuteCard, minutesCard, hoursCard),
        ),
    )

    val collection = createState(
        isBuybackSelected = false,
        content = ContentUM.Items(
            items = List(size = 6) { index -> collectionCard.copy(id = "collection_$index") }.toImmutableList(),
        ),
    )

    val refreshing = buyback.copy(isRefreshing = true)

    val loading = createState(isBuybackSelected = true, content = ContentUM.Loading)

    val buybackEmpty = createState(
        isBuybackSelected = true,
        content = ContentUM.Empty(message = resourceReference(R.string.collectibles_inventory_buyback_empty)),
    )

    val collectionEmpty = createState(
        isBuybackSelected = false,
        content = ContentUM.Empty(message = resourceReference(R.string.collectibles_inventory_collection_empty)),
    )

    val error = createState(
        isBuybackSelected = true,
        content = ContentUM.Error(
            message = resourceReference(R.string.collectibles_inventory_load_error),
            isReloading = false,
            onReloadClick = {},
        ),
    )

    val searchDefault = collection.copy(
        contentKey = "search",
        search = collection.search.copy(isActive = true, onCloseClick = {}),
        isSearchMode = true,
    )

    val searchResults = searchDefault.copy(
        content = ContentUM.Items(
            items = persistentListOf(hoursCard, collectionCard.copy(id = "collection_0"), collectionCard),
        ),
        search = searchDefault.search.copy(query = "Muk"),
    )

    val searchNoResults = searchDefault.copy(
        content = ContentUM.Empty(message = resourceReference(R.string.collectibles_main_search_empty)),
        search = searchDefault.search.copy(query = "Charizard"),
    )

    private fun createCard(id: String, timeLeft: TextReference?) = ItemUM.Card(
        id = id,
        imageUrl = "",
        title = stringReference("1999 #13 MUK-HOLO PSA 9 POKEMON"),
        price = stringReference("$192"),
        grade = stringReference("PSA 9"),
        timeLeft = timeLeft,
        onClick = {},
    )

    private fun createState(isBuybackSelected: Boolean, content: ContentUM) = CollectiblesInventoryUM(
        tabs = persistentListOf(
            TangemTabItemUM.Content(
                id = "buyback",
                label = resourceReference(R.string.collectibles_inventory_buyback_tab),
                counter = stringReference("4"),
                isSelected = isBuybackSelected,
                onClick = {},
            ),
            TangemTabItemUM.Content(
                id = "collection",
                label = resourceReference(R.string.collectibles_inventory_collection_tab),
                counter = stringReference("6"),
                isSelected = !isBuybackSelected,
                onClick = {},
            ),
        ),
        contentKey = if (isBuybackSelected) "buyback" else "collection",
        content = content,
        isRefreshing = false,
        search = TangemSearch.State(
            placeholderText = resourceReference(R.string.common_search),
            query = "",
            onQueryChange = {},
            isActive = false,
            onActiveChange = {},
        ),
        isSearchMode = false,
    )
}