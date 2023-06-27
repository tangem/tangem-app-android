package com.tangem.feature.wallet.presentation.common

import com.tangem.core.ui.R
import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.common.state.TokenItemState.TokenOptionsState
import com.tangem.feature.wallet.presentation.organizetokens.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.util.UUID

internal object WalletPreviewData {

    val walletTopBarConfig = WalletTopBarConfig(onScanCardClick = {}, onMoreClick = {})

    val walletCardContentState = WalletCardState.Content(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        balance = "8923,05 $",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardLoadingState = WalletCardState.Loading(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardHiddenContentState = WalletCardState.HiddenContent(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardErrorState = WalletCardState.Error(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val tokenItemVisibleState = TokenItemState.Content(
        id = UUID.randomUUID().toString(),
        tokenIconUrl = null,
        tokenIconResId = R.drawable.img_polygon_22,
        networkIconResId = R.drawable.img_polygon_22,
        name = "Polygon",
        amount = "5,412 MATIC",
        hasPending = true,
        tokenOptions = TokenOptionsState.Visible(
            fiatAmount = "321 $",
            priceChange = TokenOptionsState.PriceChange(
                valuePercent = "2%",
                type = TokenOptionsState.PriceChange.Type.UP,
            ),
        ),
    )

    val tokenItemHiddenState = TokenItemState.Content(
        id = UUID.randomUUID().toString(),
        tokenIconUrl = null,
        tokenIconResId = R.drawable.img_polygon_22,
        networkIconResId = R.drawable.img_polygon_22,
        name = "Polygon",
        amount = "5,412 MATIC",
        hasPending = true,
        tokenOptions = TokenOptionsState.Hidden(
            priceChange = TokenOptionsState.PriceChange(
                valuePercent = "2%",
                type = TokenOptionsState.PriceChange.Type.UP,
            ),
        ),
    )

    val tokenItemDragState = TokenItemState.Draggable(
        id = UUID.randomUUID().toString(),
        tokenIconUrl = null,
        tokenIconResId = R.drawable.img_polygon_22,
        networkIconResId = R.drawable.img_polygon_22,
        name = "Polygon",
        fiatAmount = "3 172,14 $",
    )

    val tokenItemUnreachableState = TokenItemState.Unreachable(
        id = UUID.randomUUID().toString(),
        tokenIconUrl = null,
        tokenIconResId = R.drawable.img_polygon_22,
        networkIconResId = R.drawable.img_polygon_22,
        name = "Polygon",
    )

    val loadingTokenItemState = TokenItemState.Loading(id = UUID.randomUUID().toString())

    private const val networksSize = 10
    private const val tokensSize = 3
    val draggableItems = List(networksSize) { it }
        .flatMap { index ->
            val lastNetworkIndex = networksSize - 1
            val networkNumber = index + 1

            val group = DraggableItem.GroupHeader(
                id = "group_$networkNumber",
                networkName = "$networkNumber",
            )

            val tokens: MutableList<DraggableItem.Token> = mutableListOf()
            repeat(times = tokensSize) { i ->
                val tokenNumber = i + 1
                tokens.add(
                    DraggableItem.Token(
                        tokenItemState = tokenItemDragState.copy(
                            id = "${group.id}_token_$tokenNumber",
                            name = "Token $tokenNumber",
                            networkIconResId = R.drawable.img_eth_22.takeIf { i != 0 },
                        ),
                        groupId = group.id,
                    ),
                )
            }

            val divider = DraggableItem.GroupPlaceholder(id = "divider_$networkNumber")

            buildList {
                add(group)
                addAll(tokens)
                if (index != lastNetworkIndex) {
                    add(divider)
                }
            }
        }
        .toPersistentList()

    val draggableTokens = draggableItems
        .filterIsInstance<DraggableItem.Token>()
        .toPersistentList()

    val groupedOrganizeTokensState = OrganizeTokensStateHolder(
        itemsState = OrganizeTokensListState.GroupedByNetwork(
            items = draggableItems,
        ),
        header = OrganizeTokensStateHolder.HeaderConfig(
            onSortByBalanceClick = {},
            onGroupByNetworkClick = {},
        ),
        dragConfig = OrganizeTokensStateHolder.DragConfig(
            onItemDragged = { _, _ -> },
            onDragStart = {},
            canDragItemOver = { _, _ -> false },
            onItemDragEnd = {},
        ),
        actions = OrganizeTokensStateHolder.ActionsConfig(
            onApplyClick = {},
            onCancelClick = {},
        ),
    )

    val organizeTokensState = groupedOrganizeTokensState.copy(
        itemsState = OrganizeTokensListState.Ungrouped(
            items = draggableTokens,
        ),
    )

    val multicurrencyWalletScreenState = WalletStateHolder.MultiCurrencyContent(
        onBackClick = {},
        topBarConfig = walletTopBarConfig,
        selectedWallet = walletCardContentState,
        wallets = persistentListOf(
            walletCardContentState,
            walletCardLoadingState,
            walletCardHiddenContentState,
            walletCardErrorState,
        ),
        contentItems = persistentListOf(
            WalletContentItemState.MultiCurrencyItem.NetworkGroupTitle("Bitcoin"),
            WalletContentItemState.MultiCurrencyItem.Token(
                tokenItemVisibleState.copy(
                    id = "token_1",
                    name = "Ethereum",
                    tokenIconResId = R.drawable.img_eth_22,
                    networkIconResId = null,
                    amount = "1,89340821 ETH",
                ),
            ),
            WalletContentItemState.MultiCurrencyItem.Token(
                tokenItemVisibleState.copy(
                    id = "token_2",
                    name = "Ethereum",
                    tokenIconResId = R.drawable.img_eth_22,
                    networkIconResId = null,
                    amount = "1,89340821 ETH",
                ),
            ),
            WalletContentItemState.MultiCurrencyItem.Token(
                tokenItemVisibleState.copy(
                    id = "token_3",
                    name = "Ethereum",
                    tokenIconResId = R.drawable.img_eth_22,
                    networkIconResId = null,
                    amount = "1,89340821 ETH",
                ),
            ),
            WalletContentItemState.MultiCurrencyItem.Token(
                tokenItemVisibleState.copy(
                    id = "token_4",
                    name = "Ethereum",
                    tokenIconResId = R.drawable.img_eth_22,
                    networkIconResId = null,
                    amount = "1,89340821 ETH",
                ),
            ),
            WalletContentItemState.MultiCurrencyItem.NetworkGroupTitle("Ethereum"),
            WalletContentItemState.MultiCurrencyItem.Token(
                tokenItemVisibleState.copy(
                    id = "token_5",
                    name = "Ethereum",
                    tokenIconResId = R.drawable.img_eth_22,
                    networkIconResId = null,
                    amount = "1,89340821 ETH",
                ),
            ),
        ),
        notifications = persistentListOf(
            WalletNotification.UnreachableNetworks,
            WalletNotification.LikeTangemApp(onClick = {}),
            WalletNotification.NeedToBackup(onClick = {}),
            WalletNotification.ScanCard(onClick = {}),
        ),
        onOrganizeTokensClick = {},
    )

    val singleWalletScreenState = WalletStateHolder.SingleCurrencyContent(
        onBackClick = {},
        topBarConfig = walletTopBarConfig,
        selectedWallet = walletCardContentState,
        wallets = persistentListOf(
            walletCardContentState,
            walletCardLoadingState,
            walletCardHiddenContentState,
            walletCardErrorState,
        ),
        contentItems = persistentListOf(
            WalletContentItemState.SingleCurrencyItem.Title(onExploreClick = {}),
            WalletContentItemState.SingleCurrencyItem.TransactionGroupTitle("Today"),
            WalletContentItemState.SingleCurrencyItem.Transaction(
                TransactionState.Sending(
                    address = "33BddS...ga2B",
                    amount = "-0.500913 BTC",
                    timestamp = "8:41",
                ),
            ),
            WalletContentItemState.SingleCurrencyItem.TransactionGroupTitle("Yesterday"),
            WalletContentItemState.SingleCurrencyItem.Transaction(
                TransactionState.Sending(
                    address = "33BddS...ga2B",
                    amount = "-0.500913 BTC",
                    timestamp = "8:41",
                ),
            ),
        ),
        notifications = persistentListOf(
            WalletNotification.UnreachableNetworks,
            WalletNotification.LikeTangemApp(onClick = {}),
            WalletNotification.NeedToBackup(onClick = {}),
            WalletNotification.ScanCard(onClick = {}),
        ),
    )
}