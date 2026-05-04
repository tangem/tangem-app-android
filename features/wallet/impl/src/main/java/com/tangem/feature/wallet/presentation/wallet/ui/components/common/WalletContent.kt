package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.tangem.common.ui.notifications.notifications
import com.tangem.common.ui.notifications.notificationsCarousel
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.txHistoryItems
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.tokensListItems
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.tokensListItems2
import com.tangem.feature.wallet.presentation.wallet.ui.components.nftCollections2
import com.tangem.feature.wallet.presentation.wallet.ui.components.organizeTokens2
import com.tangem.feature.wallet.presentation.wallet.ui.components.tangemPay
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun WalletListContent(
    currentWallet: WalletUM,
    isBalanceHidden: Boolean,
    listState: LazyListState,
    tangemPayComponent: TangemPayMainBlockComponent,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val containerColor = TangemTheme.colors2.surface.level1

    val movableItemModifier = Modifier.padding(horizontal = TangemTheme.dimens2.x3)
    val itemModifier = movableItemModifier.padding(top = TangemTheme.dimens2.x3)

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        overscrollEffect = rememberOverscrollEffect(),
    ) {
        notifications(
            notifications = currentWallet.notifications.map { it.messageUM }.toPersistentList(),
            contentColor = containerColor,
            modifier = movableItemModifier,
        )
        notificationsCarousel(
            containerColor = containerColor,
            modifier = movableItemModifier,
            notifications = currentWallet.notificationsCarousel.map { it.messageUM }.toPersistentList(),
        )

        tangemPay(
            tangemPayComponent = tangemPayComponent,
            tangemPayUM = currentWallet.tangemPayMainUM,
            isBalanceHidden = isBalanceHidden,
            modifier = itemModifier,
        )

        tokensListItems2(
            walletTokensListUM = currentWallet.tokensListUM,
            modifier = movableItemModifier,
            isBalanceHidden = isBalanceHidden,
        )

        nftCollections2(state = currentWallet, itemModifier = itemModifier)

        organizeTokens2(state = currentWallet, itemModifier = itemModifier)
    }
}

/**
 * Wallet content
 *
 * @param state          wallet state
 * @param txHistoryItems transaction history items
 * @param modifier       modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.contentItems(
    state: WalletState,
    txHistoryItems: LazyPagingItems<TxHistoryState.TxHistoryItemState>?,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletState.MultiCurrency -> {
            tokensListItems(state.tokensListState, modifier, isBalanceHidden)
        }
        is WalletState.SingleCurrency -> {
            txHistoryItems(state.txHistoryState, txHistoryItems, isBalanceHidden, modifier)
        }
    }
}