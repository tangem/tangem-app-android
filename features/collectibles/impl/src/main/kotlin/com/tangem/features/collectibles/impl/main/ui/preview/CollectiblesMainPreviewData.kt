package com.tangem.features.collectibles.impl.main.ui.preview

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.annotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_globe_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_mail_20
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import kotlinx.collections.immutable.persistentListOf

internal object CollectiblesMainPreviewData {

    val pack = CollectiblesMainUM.PackUM(
        id = "legendary_1",
        imageUrl = "",
        title = stringReference("Legendary 1"),
        price = stringReference("$50"),
        badge = stringReference("Buyback 85%"),
        onClick = {},
    )

    val default = CollectiblesMainUM(
        title = stringReference("Gacha"),
        balance = CollectiblesMainUM.BalanceUM(
            fiatStyled = annotatedReference(buildAnnotatedString { append("$ 2,700.52") }),
            fiatPlain = stringReference("$ 2,700.52"),
            crypto = stringReference("2,500.52 USDC"),
        ),
        actions = persistentListOf(
            CollectiblesMainUM.ActionUM(
                id = "add_funds",
                icon = Icons.ic_arrow_down_24,
                title = stringReference("Add funds"),
                onClick = {},
            ),
            CollectiblesMainUM.ActionUM(
                id = "withdrawal",
                icon = Icons.ic_arrow_up_24,
                title = stringReference("Withdrawal"),
                onClick = {},
            ),
        ),
        collection = CollectiblesMainUM.CollectionUM(
            subtitle = stringReference("My collection"),
            title = stringReference("25 cards"),
            onClick = {},
        ),
        tabs = persistentListOf(
            tab(id = "all", label = "All cards", counter = "32", isSelected = true),
            tab(id = "sports", label = "Sports", counter = "8"),
            tab(id = "pokemon", label = "Pokemon", counter = "12"),
            tab(id = "something_else", label = "Something else", counter = "6"),
            tab(id = "anime", label = "Anime", counter = "6"),
        ),
        content = CollectiblesMainUM.ContentUM.Content(
            packs = persistentListOf(
                pack,
                pack.copy(id = "water_1", title = stringReference("Water 1")),
                pack.copy(id = "elite_1", title = stringReference("Elite 1")),
                pack.copy(id = "legendary_2", title = stringReference("Legendary 2")),
            ),
        ),
        search = searchState(query = "", isActive = false),
        menu = menu(isShown = false),
        onBackClick = {},
    )

    val zero = default.copy(
        balance = CollectiblesMainUM.BalanceUM(
            fiatStyled = annotatedReference(buildAnnotatedString { append("$ 0.00") }),
            fiatPlain = stringReference("$ 0.00"),
            crypto = stringReference("0.00 USDC"),
        ),
        collection = null,
    )

    val listError = default.copy(
        tabs = persistentListOf(),
        content = CollectiblesMainUM.ContentUM.Error(
            message = stringReference("Failed to load cards list.\nTap to reload"),
            onReloadClick = {},
        ),
    )

    val searchActive = default.copy(
        tabs = persistentListOf(),
        content = CollectiblesMainUM.ContentUM.Content(
            packs = persistentListOf(
                pack,
                pack.copy(id = "water_1", title = stringReference("Water 1")),
            ),
        ),
        search = searchState(query = "Serhii", isActive = true),
    )

    val searchZero = searchActive.copy(
        content = CollectiblesMainUM.ContentUM.Empty(
            message = stringReference("No results. Try to change your wording\nor explore from the list"),
        ),
    )

    val searchError = searchActive.copy(
        content = CollectiblesMainUM.ContentUM.Error(
            message = stringReference("Failed to load cards list.\nTap to reload"),
            onReloadClick = {},
        ),
    )

    val menuShown = default.copy(menu = menu(isShown = true))

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

    private fun menu(isShown: Boolean) = CollectiblesMainUM.MenuUM(
        items = persistentListOf(
            menuItem(id = "activity_log", icon = Icons.ic_clock_20, title = "Activity log"),
            menuItem(id = "deliveries", icon = Icons.ic_globe_20, title = "Deliveries"),
            menuItem(id = "how_it_works", icon = Icons.ic_info_20, title = "How it works", hasDividerAbove = true),
            menuItem(id = "get_assistance", icon = Icons.ic_mail_20, title = "Get assistance"),
        ),
        isShown = isShown,
        onMenuClick = {},
        onDismiss = {},
    )

    private fun menuItem(id: String, icon: ImageVector, title: String, hasDividerAbove: Boolean = false) =
        CollectiblesMainUM.MenuItemUM(
            id = id,
            icon = icon,
            title = stringReference(title),
            hasDividerAbove = hasDividerAbove,
            onClick = {},
        )
}