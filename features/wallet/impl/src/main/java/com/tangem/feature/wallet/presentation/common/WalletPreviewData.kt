package com.tangem.feature.wallet.presentation.common

import androidx.paging.PagingData
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.tokenicon.TokenIconState
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.wallet.state.ActionsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.state.TokenActionButtonConfig
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState.TokensListItemState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

@Suppress("LargeClass")
internal object WalletPreviewData {

    val topBarConfig by lazy { WalletTopBarConfig(onDetailsClick = {}) }

    val walletCardContentState by lazy {
        WalletCardState.Content(
            id = UserWalletId(stringValue = "123"),
            title = "Wallet1Wallet1Wallet1Wallet1Wallet1Wallet1Wallet1Wallet1",
            balance = "8923,05312312312312312312331231231233432423423424234 $",
            additionalInfo = WalletAdditionalInfo(
                hideable = false,
                content = TextReference.Str("3 cards • Seed phrase3 cards • Seed phrasephrasephrasephrase"),
            ),
            imageResId = R.drawable.ill_wallet2_cards3_120_106,
            onRenameClick = { _ -> },
            onDeleteClick = {},
            cardCount = 1,
        )
    }

    val walletCardLoadingState by lazy {
        WalletCardState.Loading(
            id = UserWalletId("321"),
            title = "Wallet 1",
            imageResId = R.drawable.ill_wallet2_cards3_120_106,
            onRenameClick = { _ -> },
            onDeleteClick = {},
        )
    }

    val walletCardErrorState by lazy {
        WalletCardState.Error(
            id = UserWalletId("24"),
            title = "Wallet 1",
            imageResId = R.drawable.ill_wallet2_cards3_120_106,
            onRenameClick = { _ -> },
            onDeleteClick = {},
        )
    }

    val wallets by lazy {
        mapOf(
            UserWalletId(stringValue = "123") to walletCardContentState,
            UserWalletId(stringValue = "321") to walletCardLoadingState,
            UserWalletId(stringValue = "24") to walletCardErrorState,
        )
    }

    val walletListConfig by lazy {
        WalletsListConfig(
            selectedWalletIndex = 0,
            wallets = wallets.values.toPersistentList(),
            onWalletChange = {},
        )
    }

    val coinIconState
        get() = TokenIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_polygon_22,
            isGrayscale = false,
            showCustomBadge = false,
        )

    private val tokenIconState
        get() = TokenIconState.TokenIcon(
            url = null,
            networkBadgeIconResId = R.drawable.img_polygon_22,
            fallbackTint = TangemColorPalette.Black,
            fallbackBackground = TangemColorPalette.Meadow,
            isGrayscale = false,
            showCustomBadge = false,
        )

    private val customTokenIconState
        get() = TokenIconState.CustomTokenIcon(
            tint = TangemColorPalette.Black,
            background = TangemColorPalette.Meadow,
            networkBadgeIconResId = R.drawable.img_polygon_22,
            isGrayscale = false,
        )

    val tokenItemVisibleState by lazy {
        TokenItemState.Content(
            id = UUID.randomUUID().toString(),
            iconState = coinIconState,
            titleState = TokenItemState.TitleState.Content(text = "Polygon", hasPending = true),
            fiatAmountState = TokenItemState.FiatAmountState.Content(text = "321 $"),
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = "5,412 MATIC"),
            cryptoPriceState = TokenItemState.CryptoPriceState.Unknown,
            onItemClick = {},
            onItemLongClick = {},
        )
    }

    val testnetTokenItemVisibleState by lazy {
        tokenItemVisibleState.copy(
            titleState = TokenItemState.TitleState.Content(text = "Polygon testnet"),
            iconState = tokenIconState.copy(isGrayscale = true),
        )
    }

    val tokenItemHiddenState by lazy {
        TokenItemState.Content(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = "Polygon"),
            fiatAmountState = TokenItemState.FiatAmountState.Content(text = "321 $"),
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = "5,412 MATIC"),
            cryptoPriceState = TokenItemState.CryptoPriceState.Content(
                price = "312 USD",
                priceChangePercent = "2.0%",
                type = PriceChangeType.UP,
            ),
            onItemClick = {},
            onItemLongClick = {},
        )
    }

    val tokenItemDragState by lazy {
        TokenItemState.Draggable(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = "Polygon"),
            cryptoAmountState = TokenItemState.CryptoAmountState.Content(text = "3 172,14 $"),
        )
    }

    val tokenItemUnreachableState by lazy {
        TokenItemState.Unreachable(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = "Polygon"),
            onItemClick = {},
            onItemLongClick = {},
        )
    }

    val tokenItemNoAddressState by lazy {
        TokenItemState.NoAddress(
            id = UUID.randomUUID().toString(),
            iconState = tokenIconState,
            titleState = TokenItemState.TitleState.Content(text = "Polygon"),
            onItemLongClick = {},
        )
    }

    val customTokenItemVisibleState by lazy {
        tokenItemVisibleState.copy(
            titleState = TokenItemState.TitleState.Content(text = "Polygon"),
            iconState = customTokenIconState.copy(
                tint = TangemColorPalette.White,
                background = TangemColorPalette.Black,
            ),
        )
    }

    val customTestnetTokenItemVisibleState by lazy {
        tokenItemVisibleState.copy(
            titleState = TokenItemState.TitleState.Content(text = "Polygon"),
            iconState = customTokenIconState.copy(isGrayscale = true),
        )
    }

    val loadingTokenItemState by lazy {
        TokenItemState.Loading(
            id = "Loading#1",
            iconState = customTokenIconState.copy(isGrayscale = true),
            titleState = TokenItemState.TitleState.Content(text = "Polygon"),
        )
    }

    private const val networksSize = 10
    private const val tokensSize = 3
    private val draggableItems by lazy {
        List(networksSize) { it }
            .flatMap { index ->
                val lastNetworkIndex = networksSize - 1
                val lastTokenIndex = tokensSize - 1
                val networkNumber = index + 1

                val group = DraggableItem.GroupHeader(
                    id = networkNumber,
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
                                titleState = TokenItemState.TitleState.Content(
                                    text = "Token $tokenNumber from $networkNumber network",
                                ),
                            ),
                            groupId = group.id,
                            roundingMode = when {
                                i == lastTokenIndex && index == lastNetworkIndex -> DraggableItem.RoundingMode.Bottom()
                                else -> DraggableItem.RoundingMode.None
                            },
                        ),
                    )
                }

                val divider = DraggableItem.Placeholder(id = "divider_$networkNumber")

                buildList {
                    add(group)
                    addAll(tokens)
                    if (index != lastNetworkIndex) {
                        add(divider)
                    }
                }
            }
            .toPersistentList()
    }

    private val draggableTokens by lazy {
        draggableItems
            .filterIsInstance<DraggableItem.Token>()
            .toMutableList()
            .also {
                it[0] = it[0].copy(roundingMode = DraggableItem.RoundingMode.Top())
            }
            .toPersistentList()
    }

    val groupedOrganizeTokensState by lazy {
        OrganizeTokensState(
            onBackClick = {},
            itemsState = OrganizeTokensListState.GroupedByNetwork(
                items = draggableItems,
            ),
            header = OrganizeTokensState.HeaderConfig(
                onSortClick = {},
                onGroupClick = {},
            ),
            dndConfig = OrganizeTokensState.DragAndDropConfig(
                onItemDragged = { _, _ -> },
                onItemDragStart = {},
                canDragItemOver = { _, _ -> false },
                onItemDragEnd = {},
            ),
            actions = OrganizeTokensState.ActionsConfig(
                onApplyClick = {},
                onCancelClick = {},
            ),
            scrollListToTop = consumedEvent(),
            isBalanceHidden = true,
        )
    }

    val organizeTokensState by lazy {
        groupedOrganizeTokensState.copy(
            itemsState = OrganizeTokensListState.Ungrouped(
                items = draggableTokens,
            ),
        )
    }

    val bottomSheet by lazy {
        TangemBottomSheetConfig(
            isShow = false,
            onDismissRequest = {},
            content = WalletBottomSheetConfig.UnlockWallets(
                onUnlockClick = {},
                onScanClick = {},
            ),
        )
    }

    val actionsBottomSheet = ActionsBottomSheetConfig(
        actions = listOf(
            TokenActionButtonConfig(
                text = TextReference.Str("Send"),
                iconResId = R.drawable.ic_share_24,
                isWarning = false,
                onClick = {},
            ),
        ).toImmutableList(),
    )

    private val manageButtons by lazy {
        persistentListOf(
            WalletManageButton.Buy(enabled = true, onClick = {}),
            WalletManageButton.Send(enabled = true, onClick = {}),
            WalletManageButton.Receive(enabled = true, onClick = {}),
            WalletManageButton.Sell(enabled = true, onClick = {}),
            WalletManageButton.Swap(enabled = true, onClick = {}),
        )
    }

    val multicurrencyWalletScreenState by lazy {
        WalletMultiCurrencyState.Content(
            onBackClick = {},
            topBarConfig = topBarConfig,
            walletsListConfig = walletListConfig,
            tokensListState = WalletTokensListState.Content(
                persistentListOf(
                    TokensListItemState.NetworkGroupTitle(id = 0, stringReference("Bitcoin")),
                    TokensListItemState.Token(
                        tokenItemVisibleState.copy(
                            id = "token_1",
                            titleState = TokenItemState.TitleState.Content(text = "Ethereum"),
                            cryptoAmountState = TokenItemState.CryptoAmountState.Content("1,89340821 ETH"),
                        ),
                    ),
                    TokensListItemState.Token(
                        tokenItemVisibleState.copy(
                            id = "token_2",
                            titleState = TokenItemState.TitleState.Content(text = "Ethereum"),
                            cryptoAmountState = TokenItemState.CryptoAmountState.Content("1,89340821 ETH"),
                        ),
                    ),
                    TokensListItemState.Token(
                        tokenItemVisibleState.copy(
                            id = "token_3",
                            titleState = TokenItemState.TitleState.Content(text = "Ethereum"),
                            cryptoAmountState = TokenItemState.CryptoAmountState.Content("1,89340821 ETH"),
                        ),
                    ),
                    TokensListItemState.Token(
                        tokenItemVisibleState.copy(
                            id = "token_4",
                            titleState = TokenItemState.TitleState.Content(text = "Ethereum"),
                            cryptoAmountState = TokenItemState.CryptoAmountState.Content("1,89340821 ETH"),
                        ),
                    ),
                    TokensListItemState.NetworkGroupTitle(id = 1, stringReference("Ethereum")),
                    TokensListItemState.Token(
                        tokenItemVisibleState.copy(
                            id = "token_5",
                            titleState = TokenItemState.TitleState.Content(text = "Ethereum"),
                            cryptoAmountState = TokenItemState.CryptoAmountState.Content("1,89340821 ETH"),
                        ),
                    ),
                ),
                organizeTokensButton = WalletTokensListState.OrganizeTokensButtonState.Visible(isEnabled = true, {}),
            ),
            pullToRefreshConfig = WalletPullToRefreshConfig(
                isRefreshing = false,
                onRefresh = {},
            ),
            notifications = persistentListOf(
                WalletNotification.Critical.DevCard,
                WalletNotification.Informational.MissingAddresses(missingAddressesCount = 0, onGenerateClick = {}),
                WalletNotification.Warning.NetworksUnreachable,
            ),
            bottomSheetConfig = bottomSheet,
            onManageTokensClick = {},
            event = consumedEvent(),
            isBalanceHidden = false,
        )
    }

    val singleWalletScreenState by lazy {
        WalletSingleCurrencyState.Content(
            onBackClick = {},
            topBarConfig = topBarConfig,
            walletsListConfig = walletListConfig,
            pullToRefreshConfig = WalletPullToRefreshConfig(
                isRefreshing = false,
                onRefresh = {},
            ),
            notifications = persistentListOf(WalletNotification.Warning.NetworksUnreachable),
            buttons = manageButtons,
            bottomSheetConfig = bottomSheet,
            marketPriceBlockState = MarketPriceBlockState.Content(
                currencySymbol = "BTC",
                price = "98900.12$",
                priceChangeConfig = PriceChangeState.Content(
                    valueInPercent = "5.16%",
                    type = PriceChangeType.UP,
                ),
            ),
            txHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    PagingData.from(
                        listOf(
                            TxHistoryState.TxHistoryItemState.GroupTitle(
                                title = "Today",
                                itemKey = UUID.randomUUID().toString(),
                            ),
                            TxHistoryState.TxHistoryItemState.Transaction(
                                TransactionState.Content(
                                    txHash = UUID.randomUUID().toString(),
                                    amount = "-0.500913 BTC",
                                    timestamp = "8:41",
                                    status = TransactionState.Content.Status.Unconfirmed,
                                    direction = TransactionState.Content.Direction.OUTGOING,
                                    iconRes = com.tangem.core.ui.R.drawable.ic_arrow_up_24,
                                    title = resourceReference(com.tangem.core.ui.R.string.common_transfer),
                                    subtitle = TextReference.Str("33BddS...ga2B"),
                                    onClick = {},
                                ),
                            ),
                            TxHistoryState.TxHistoryItemState.GroupTitle(
                                title = "Yesterday",
                                itemKey = UUID.randomUUID().toString(),
                            ),
                            TxHistoryState.TxHistoryItemState.Transaction(
                                TransactionState.Content(
                                    txHash = UUID.randomUUID().toString(),
                                    amount = "-0.500913 BTC",
                                    timestamp = "8:41",
                                    status = TransactionState.Content.Status.Confirmed,
                                    direction = TransactionState.Content.Direction.OUTGOING,
                                    iconRes = com.tangem.core.ui.R.drawable.ic_arrow_up_24,
                                    title = resourceReference(com.tangem.core.ui.R.string.common_transfer),
                                    subtitle = TextReference.Str("33BddS...ga2B"),
                                    onClick = {},
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            event = consumedEvent(),
            isBalanceHidden = false,
        )
    }
}