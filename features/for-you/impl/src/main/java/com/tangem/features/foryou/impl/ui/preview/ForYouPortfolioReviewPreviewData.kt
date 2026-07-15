package com.tangem.features.foryou.impl.ui.preview

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.DonutChartUM
import com.tangem.features.foryou.impl.components.state.DonutSegmentColor
import com.tangem.features.foryou.impl.components.state.DonutSegmentUM
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.utils.StringsSigns.DOT
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal object ForYouPortfolioReviewPreviewData {

    val reviewContent = PortfolioReviewUM.Content(
        marketChartUM = MarketChartUM.Loaded(
            donutChart = DonutChartUM.Loaded(
                totalAmount = "10000$",
                // Colours are assigned in segment order (rank), matching the transformer's palette-by-index.
                donutSegmentList = persistentListOf(
                    DonutSegmentUM(
                        color = DonutSegmentColor.Brand,
                        weight = BigDecimal("0.55"),
                        title = stringReference("Ethereum"),
                        fiatValue = stringReference("\$5,720.22"),
                    ),
                    DonutSegmentUM(
                        color = DonutSegmentColor.Green,
                        weight = BigDecimal("0.45"),
                        title = stringReference("Solana"),
                        fiatValue = stringReference("\$728.30"),
                    ),
                ),
            ),
            topHoldingPercent = stringReference("Top holding 42%"),
        ),
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
        onAddFundsClick = null,
    )

    val loadingState = PortfolioReviewUM.Loading(
        marketChartUM = MarketChartUM.NoData(
            title = resourceReference(R.string.market_chart_can_not_load_data),
            donutText = resourceReference(R.string.market_chart_bubble_no_data),
        ),
        tokenList = buildList {
            repeat(5) { index ->
                add(
                    ForYouTokenListItemUM(
                        tokenRowUM = TangemTokenRowUM.Loading(
                            id = index.toString(),
                        ),
                        tokenList = persistentListOf(),
                        isExpanded = false,
                        isExpandable = false,
                    ),
                )
            }
        }.toPersistentList(),
    )

    val zeroPortfolioState = PortfolioReviewUM.Content(
        marketChartUM = MarketChartUM.NoData(
            title = stringReference("You don’t have any tokens with amount"),
            donutText = stringReference("No amount on tokens"),
        ),
        tokenList = buildList {
            repeat(5) { index ->
                add(
                    ForYouTokenListItemUM(
                        tokenRowUM = TangemTokenRowUM.Content(
                            id = "token_$index",
                            headIconUM = TangemIconUM.Currency(CurrencyIconState.Loading),
                            titleUM = TangemTokenRowUM.TitleUM.Content(
                                text = stringReference("Token $index"),
                                badge = TangemBadgeUM(
                                    text = stringReference("Positive"),
                                    size = TangemBadgeSize.X4,
                                    type = TangemBadgeType.Tinted,
                                    color = TangemBadgeColor.Green,
                                ),
                            ),
                            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                                text = stringReference("Some network"),
                            ),
                            topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                                text = stringReference("\$0"),
                            ),
                            bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                                text = stringReference("0.00%"),
                            ),
                            onItemClick = {},
                            onItemLongClick = { _, _ -> },
                        ),
                        tokenList = persistentListOf(),
                        isExpanded = false,
                        isExpandable = false,
                    ),
                )
            }
        }.toPersistentList(),
        onAddFundsClick = { },
    )
}