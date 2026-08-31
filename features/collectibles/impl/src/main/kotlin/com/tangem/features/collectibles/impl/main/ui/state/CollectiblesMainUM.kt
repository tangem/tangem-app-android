package com.tangem.features.collectibles.impl.main.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * @property balance account balance, shown in the header and — while the list is scrolled — as the top bar subtitle.
 * @property collection the "My collection" widget; `null` hides it, as in the zero-balance design.
 * @property tabs category filter; empty while there is nothing to filter.
 * @property search hoisted state of the footer search field; [TangemSearch.State.isActive] also drives whether
 *   the header and the tabs are laid out at all.
 */
internal data class CollectiblesMainUM(
    val title: TextReference,
    val balance: BalanceUM,
    val actions: ImmutableList<ActionUM>,
    val collection: CollectionUM?,
    val tabs: ImmutableList<TangemTabItemUM>,
    val content: ContentUM,
    val search: TangemSearch.State,
    val menu: MenuUM,
    val onBackClick: () -> Unit,
) {

    /**
     * @property fiatStyled fiat amount whose fractional part carries its own span, for the large header value.
     * @property fiatPlain the same amount unstyled, for the top bar subtitle.
     */
    data class BalanceUM(
        val fiatStyled: TextReference,
        val fiatPlain: TextReference,
        val crypto: TextReference,
    )

    data class ActionUM(
        val id: String,
        val icon: ImageVector,
        val title: TextReference,
        val onClick: () -> Unit,
    )

    data class CollectionUM(
        val subtitle: TextReference,
        val title: TextReference,
        val onClick: () -> Unit,
    )

    @Immutable
    sealed interface ContentUM {

        data object Loading : ContentUM

        data class Content(val packs: ImmutableList<PackUM>) : ContentUM

        data class Error(val message: TextReference, val onReloadClick: () -> Unit) : ContentUM

        data class Empty(val message: TextReference) : ContentUM
    }

    data class PackUM(
        val id: String,
        val imageUrl: String,
        val title: TextReference,
        val price: TextReference,
        val badge: TextReference?,
        val onClick: () -> Unit,
    )

    data class MenuUM(
        val items: ImmutableList<MenuItemUM>,
        val isShown: Boolean,
        val onMenuClick: () -> Unit,
        val onDismiss: () -> Unit,
    )

    data class MenuItemUM(
        val id: String,
        val icon: ImageVector,
        val title: TextReference,
        val hasDividerAbove: Boolean,
        val onClick: () -> Unit,
    )
}