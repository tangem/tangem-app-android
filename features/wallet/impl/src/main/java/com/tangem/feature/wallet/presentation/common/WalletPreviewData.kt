package com.tangem.feature.wallet.presentation.common

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensState
import com.tangem.feature.wallet.presentation.wallet.state.model.*
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
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

    val coinIconState
        get() = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_polygon_22,
            isGrayscale = false,
            showCustomBadge = false,
        )

    private val tokenIconState
        get() = CurrencyIconState.TokenIcon(
            url = null,
            topBadgeIconResId = R.drawable.img_polygon_22,
            fallbackTint = TangemColorPalette.Black,
            fallbackBackground = TangemColorPalette.Meadow,
            isGrayscale = false,
            showCustomBadge = false,
        )

    private val customTokenIconState
        get() = CurrencyIconState.CustomTokenIcon(
            tint = TangemColorPalette.Black,
            background = TangemColorPalette.Meadow,
            topBadgeIconResId = R.drawable.img_polygon_22,
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
}