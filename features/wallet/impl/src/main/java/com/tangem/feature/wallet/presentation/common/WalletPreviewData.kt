package com.tangem.feature.wallet.presentation.common

import com.tangem.core.ui.R
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeConfig
import com.tangem.core.ui.components.transactions.TransactionState
import com.tangem.domain.wallets.models.UserWalletId
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
        onClick = null,
    )

    val walletCardLoadingState = WalletCardState.Loading(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = null,
    )

    val walletCardHiddenContentState = WalletCardState.HiddenContent(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = null,
    )

    val walletCardErrorState = WalletCardState.Error(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = null,
    )

    val wallets = mapOf(
        UserWalletId(stringValue = "123") to walletCardContentState,
        UserWalletId(stringValue = "321") to walletCardLoadingState,
        UserWalletId(stringValue = "42") to walletCardHiddenContentState,
        UserWalletId(stringValue = "24") to walletCardErrorState,
    )

    val walletListConfig = WalletsListConfig(
        selectedWalletIndex = 0,
        wallets = wallets.values.toPersistentList(),
        onWalletChange = {},
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
            priceChange = PriceChangeConfig(
                valueInPercent = "2%",
                type = PriceChangeConfig.Type.UP,
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
            priceChange = PriceChangeConfig(
                valueInPercent = "2%",
                type = PriceChangeConfig.Type.UP,
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

    val loadingTokenItemState = TokenItemState.Loading

    private const val networksSize = 10
    private const val tokensSize = 3
    val draggableItems = List(networksSize) { it }
        .flatMap { index ->
            val lastNetworkIndex = networksSize - 1
            val lastTokenIndex = tokensSize - 1
            val networkNumber = index + 1

            val group = DraggableItem.GroupHeader(
                id = "group_$networkNumber",
                networkName = "$networkNumber",
                roundingMode = when (index) {
                    0 -> DraggableItem.RoundingMode.Top()
                    lastNetworkIndex -> DraggableItem.RoundingMode.Bottom()
                    else -> DraggableItem.RoundingMode.None
                },
            )

            val tokens: MutableList<DraggableItem.Token> = mutableListOf()
            repeat(times = tokensSize) { i ->
                val tokenNumber = i + 1
                tokens.add(
                    DraggableItem.Token(
                        tokenItemState = tokenItemDragState.copy(
                            id = "${group.id}_token_$tokenNumber",
                            name = "Token $tokenNumber from $networkNumber network",
                            networkIconResId = R.drawable.img_eth_22.takeIf { i != 0 },
                        ),
                        groupId = group.id,
                        roundingMode = when {
                            i == lastTokenIndex && index == lastNetworkIndex -> DraggableItem.RoundingMode.Bottom()
                            else -> DraggableItem.RoundingMode.None
                        },
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
        .toMutableList()
        .also {
            it[0] = it[0].copy(roundingMode = DraggableItem.RoundingMode.Top())
        }
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

    private val manageButtons = persistentListOf(
        WalletManageButton.Buy(onClick = {}),
        WalletManageButton.Send(onClick = {}),
        WalletManageButton.Receive(onClick = {}),
        WalletManageButton.Exchange(onClick = {}),
        WalletManageButton.CopyAddress(onClick = {}),
    )

    val multicurrencyWalletScreenState = WalletStateHolder.MultiCurrencyContent(
        onBackClick = {},
        topBarConfig = walletTopBarConfig,
        walletsListConfig = walletListConfig,
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
        pullToRefreshConfig = WalletPullToRefreshConfig(
            isRefreshing = false,
            onRefresh = {},
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
        walletsListConfig = walletListConfig,
        contentItems = persistentListOf(
            WalletContentItemState.SingleCurrencyItem.Title(onExploreClick = {}),
            WalletContentItemState.SingleCurrencyItem.GroupTitle("Today"),
            WalletContentItemState.SingleCurrencyItem.Transaction(
                TransactionState.Sending(
                    address = "33BddS...ga2B",
                    amount = "-0.500913 BTC",
                    timestamp = "8:41",
                ),
            ),
            WalletContentItemState.SingleCurrencyItem.GroupTitle("Yesterday"),
            WalletContentItemState.SingleCurrencyItem.Transaction(
                TransactionState.Sending(
                    address = "33BddS...ga2B",
                    amount = "-0.500913 BTC",
                    timestamp = "8:41",
                ),
            ),
        ),
        pullToRefreshConfig = WalletPullToRefreshConfig(
            isRefreshing = false,
            onRefresh = {},
        ),
        notifications = persistentListOf(WalletNotification.LikeTangemApp(onClick = {})),
        buttons = manageButtons.map(WalletManageButton::config).toPersistentList(),
        marketPriceBlockState = MarketPriceBlockState.Content(
            currencyName = "BTC",
            price = "98900.12$",
            priceChangeConfig = PriceChangeConfig(
                valueInPercent = "5.16%",
                type = PriceChangeConfig.Type.UP,
            ),
        ),
    )
}