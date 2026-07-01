package com.tangem.features.foryou.impl.ui.preview

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.utils.StringsSigns.DOT
import kotlinx.collections.immutable.persistentListOf

internal object ForYouPortfolioReviewPreviewData {

    val reviewContent = PortfolioReviewUM.Content(
        assetCount = stringReference("5 assets"),
        topHoldingPercent = stringReference("Top holding 42%"),
        periodPickerUM = TangemSegmentedPickerUM(
            items = persistentListOf(
                TangemSegmentUM(id = "0", title = stringReference("Day")),
                TangemSegmentUM(id = "1", title = stringReference("Week")),
                TangemSegmentUM(id = "2", title = stringReference("Month")),
            ),
            initialSelectedItem = TangemSegmentUM(id = "0", title = stringReference("Day")),
            isFixed = true,
            isAltSurface = true,
        ),
        onPeriodClick = {},
        tokenList = persistentListOf(
            ForYouTokenListItemUM(
                tokenRowUM = TangemTokenRowUM.Content(
                    id = "network_0",
                    headIconUM = TangemIconUM.Currency(CurrencyIconState.Loading),
                    titleUM = TangemTokenRowUM.TitleUM.Content(
                        text = stringReference("USDC"),
                        badge = TangemBadgeUM(
                            text = stringReference("Positive"),
                            size = TangemBadgeSize.X4,
                            type = TangemBadgeType.Tinted,
                            color = TangemBadgeColor.Green,
                        ),
                    ),
                    subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                        text = stringReference("2 networks"),
                    ),
                    topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                        text = stringReference("\$5,479"),
                    ),
                    bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                        text = stringReference("54,8%"),
                    ),
                    onItemClick = {},
                    onItemLongClick = { _, _ -> },
                ),
                tokenList = persistentListOf(
                    TangemTokenRowUM.Content(
                        id = "network_0_token_0",
                        headIconUM = TangemIconUM.Currency(CurrencyIconState.Loading),
                        titleUM = TangemTokenRowUM.TitleUM.Content(
                            text = stringReference("USDC"),
                            badge = TangemBadgeUM(
                                text = stringReference("Positive"),
                                size = TangemBadgeSize.X4,
                                type = TangemBadgeType.Tinted,
                                color = TangemBadgeColor.Green,
                            ),
                        ),
                        subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                            text = stringReference("Solana $DOT 3,479 USDC"),
                        ),
                        topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                            text = stringReference("\$3,479"),
                        ),
                        bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                            text = stringReference("34,7%"),
                        ),
                        onItemClick = {},
                        onItemLongClick = { _, _ -> },
                    ),
                    TangemTokenRowUM.Content(
                        id = "network_0_token_1",
                        headIconUM = TangemIconUM.Currency(CurrencyIconState.Loading),
                        titleUM = TangemTokenRowUM.TitleUM.Content(
                            text = stringReference("USDC"),
                            badge = TangemBadgeUM(
                                text = stringReference("Positive"),
                                size = TangemBadgeSize.X4,
                                type = TangemBadgeType.Tinted,
                                color = TangemBadgeColor.Green,
                            ),
                        ),
                        subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                            text = stringReference("Ethereum $DOT 2,000 USDC"),
                        ),
                        topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                            text = stringReference("\$2,000"),
                        ),
                        bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                            text = stringReference("20,1%"),
                        ),
                        onItemClick = {},
                        onItemLongClick = { _, _ -> },
                    ),
                ),
                isExpanded = true,
                isExpandable = true,
            ),
            ForYouTokenListItemUM(
                tokenRowUM = TangemTokenRowUM.Content(
                    id = "network_1",
                    headIconUM = TangemIconUM.Currency(CurrencyIconState.Loading),
                    titleUM = TangemTokenRowUM.TitleUM.Content(
                        text = stringReference("Bitcoin"),
                        badge = TangemBadgeUM(
                            text = stringReference("Positive"),
                            size = TangemBadgeSize.X4,
                            type = TangemBadgeType.Tinted,
                            color = TangemBadgeColor.Green,
                        ),
                    ),
                    subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                        text = stringReference("Main network"),
                    ),
                    topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                        text = stringReference("\$849"),
                    ),
                    bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                        text = stringReference("8,49%"),
                    ),
                    onItemClick = {},
                    onItemLongClick = { _, _ -> },
                ),
                tokenList = persistentListOf(),
                isExpanded = false,
                isExpandable = false,
            ),
        ),
    )
}