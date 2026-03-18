package com.tangem.feature.wallet.child.organizetokens.ui.preview

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.internal.TangemRowTailUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.feature.wallet.child.organizetokens.entity.RoundingModeUM
import com.tangem.feature.wallet.impl.R
import kotlinx.collections.immutable.toPersistentList
import java.util.UUID

internal object OrganizeTokensPreview {

    private const val networksSize = 10
    private const val tokensSize = 3

    private val draggableToken = TangemTokenRowUM.Actionable(
        id = UUID.randomUUID().toString(),
        headIconUM = TangemIconUM.Currency(
            CurrencyIconState.TokenIcon(
                url = null,
                topBadgeIconResId = R.drawable.img_polygon_22,
                fallbackTint = TangemColorPalette.Black,
                fallbackBackground = TangemColorPalette.Meadow,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
        ),
        titleUM = TangemTokenRowUM.TitleUM.Content(stringReference(value = "Polygon")),
        subtitleUM = TangemTokenRowUM.SubtitleUM.Empty,
        topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
            text = stringReference("$ 42,900.13"),
        ),
        bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
            text = stringReference("733,71097 POL"),
        ),
        tailUM = TangemRowTailUM.Draggable(R.drawable.ic_group_drop_24),
        onItemClick = null,
        onItemLongClick = null,
    )

    private val tokenList = List(networksSize) { it }
        .flatMap { index ->
            val lastNetworkIndex = networksSize - 1
            val lastTokenIndex = tokensSize - 1
            val networkNumber = index + 1

            val group = OrganizeRowItemUM.Network(
                headerRowUM = TangemHeaderRowUM(
                    id = networkNumber.toString(),
                    title = stringReference(value = "$networkNumber"),
                ),
                roundingModeUM = when (index) {
                    0 -> RoundingModeUM.Top()
                    lastNetworkIndex -> RoundingModeUM.Bottom()
                    else -> RoundingModeUM.None
                },
                accountId = "account_$networkNumber",
            )

            val tokens: MutableList<OrganizeRowItemUM.Token> = mutableListOf()
            repeat(times = tokensSize) { i ->
                val tokenNumber = i + 1
                tokens.add(
                    OrganizeRowItemUM.Token(
                        tokenRowUM = draggableToken.copy(
                            id = "${group.id}_token_$tokenNumber",
                            titleUM = TangemTokenRowUM.TitleUM.Content(
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

            val divider = OrganizeRowItemUM.Placeholder(
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

    val defaultState by lazy {
        OrganizeTokensUM(
            tokenList = tokenList,
            organizeMenuUM = OrganizeTokensUM.OrganizeMenuUM(
                onSortClick = {},
                onGroupClick = {},
            ),
            cancelButton = TangemButtonUM(
                text = resourceReference(R.string.common_cancel),
                onClick = {},
                type = TangemButtonType.Secondary,
            ),
            applyButton = TangemButtonUM(
                text = resourceReference(R.string.common_apply),
                onClick = {},
                type = TangemButtonType.Primary,
            ),
            scrollListToTop = consumedEvent(),
            isAccountsMode = true,
            isBalanceHidden = true,
            isGrouped = false,
        )
    }
}