package com.tangem.feature.wallet.presentation.common

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.impl.R
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
            isZeroBalance = false,
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

    private val tokenItemDragState by lazy {
        TokenItemState.Draggable(
            id = UUID.randomUUID().toString(),
            iconState = CurrencyIconState.TokenIcon(
                url = null,
                topBadgeIconResId = R.drawable.img_polygon_22,
                fallbackTint = TangemColorPalette.Black,
                fallbackBackground = TangemColorPalette.Meadow,
                isGrayscale = false,
                showCustomBadge = false,
            ),
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "3 172,14 $"),
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
                                    text = stringReference(value = "Token $tokenNumber from $networkNumber network"),
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