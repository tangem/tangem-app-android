package com.tangem.feature.wallet.child.organizetokens.ui.preview

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensListUM
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensState
import com.tangem.feature.wallet.child.organizetokens.entity.RoundingModeUM
import com.tangem.feature.wallet.impl.R
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import java.util.UUID

internal object OrganizeTokensPreviewLegacy {

    private const val networksSize = 10
    private const val tokensSize = 3

    private val tokenItemDragState by lazy {
        TokenItemState.Draggable(
            id = UUID.randomUUID().toString(),
            iconState = CurrencyIconState.TokenIcon(
                url = null,
                topBadgeIconResId = R.drawable.img_polygon_22,
                fallbackTint = TangemColorPalette.Black,
                fallbackBackground = TangemColorPalette.Meadow,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
            titleState = TokenItemState.TitleState.Content(text = stringReference(value = "Polygon")),
            subtitle2State = TokenItemState.Subtitle2State.TextContent(text = "3 172,14 $"),
        )
    }

    private val draggableItems: PersistentList<DraggableItem> by lazy {
        List(networksSize) { it }
            .flatMap { index ->
                val lastNetworkIndex = networksSize - 1
                val lastTokenIndex = tokensSize - 1
                val networkNumber = index + 1

                val group = DraggableItem.GroupHeader(
                    id = networkNumber,
                    networkName = "$networkNumber",
                    roundingModeUM = when (index) {
                        0 -> RoundingModeUM.Top()
                        lastNetworkIndex -> RoundingModeUM.Bottom()
                        else -> RoundingModeUM.None
                    },
                    accountId = "account_$networkNumber",
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
                            accountId = "account_$networkNumber",
                            roundingModeUM = when {
                                i == lastTokenIndex && index == lastNetworkIndex -> RoundingModeUM.Bottom()
                                else -> RoundingModeUM.None
                            },
                        ),
                    )
                }

                val divider = DraggableItem.Placeholder(
                    id = "divider_$networkNumber",
                    accountId = "account_$networkNumber",
                )

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

    val stateAccounts by lazy {
        OrganizeTokensState(
            onBackClick = {},
            tokenListUM = OrganizeTokensListUM.AccountList(
                items = draggableItems,
                isGrouped = true,
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

    val state by lazy {
        stateAccounts.copy(
            tokenListUM = OrganizeTokensListUM.TokensList(
                items = draggableItems,
                isGrouped = true,
            ),
        )
    }
}