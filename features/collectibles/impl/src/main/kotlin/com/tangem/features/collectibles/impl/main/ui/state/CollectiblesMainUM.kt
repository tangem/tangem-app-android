package com.tangem.features.collectibles.impl.main.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class CollectiblesMainUM(
    val title: TextReference,
    val balance: BalanceUM,
    val actions: ImmutableList<TangemButtonUM>,
    val collection: CollectionUM?,
    val relink: RelinkUM,
    val pendingPacks: ImmutableList<PendingPackUM>,
    val menu: MenuUM,
    val isBalanceRefreshing: Boolean,
    val onBackClick: () -> Unit,
) {

    data class BalanceUM(
        val fiatStyled: TextReference,
        val fiatPlain: TextReference,
        val crypto: TextReference,
    )

    data class CollectionUM(
        val subtitle: TextReference,
        val title: TextReference,
        val onClick: () -> Unit,
    )

    @Immutable
    sealed interface RelinkUM {
        data object Hidden : RelinkUM
        data class Shown(val isLoading: Boolean, val onRelinkClick: () -> Unit) : RelinkUM
    }

    /**
     * One banner per pending purchase.
     *
     * @property icon a status glyph at the end; a banner the user can close shows the close button instead
     */
    @Immutable
    data class PendingPackUM(
        val txId: String,
        val title: TextReference,
        val description: TextReference,
        val icon: ImageVector?,
        val onCloseClick: (() -> Unit)?,
        val button: TangemMessageBanner.Button?,
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