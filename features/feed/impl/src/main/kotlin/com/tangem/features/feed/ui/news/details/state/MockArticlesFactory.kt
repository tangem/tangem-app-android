package com.tangem.features.feed.ui.news.details.state

import com.tangem.core.ui.components.label.entity.LabelSize
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Suppress("MaximumLineLength", "LongMethod")
internal object MockArticlesFactory {
    fun createMockArticles(): ImmutableList<ArticleUM> = listOf(
        ArticleUM(
            id = 1,
            title = "SEC delays decisions on ETH-staking ETFs and spot XRP/SOL funds",
            createdAt = TextReference.Str("20 Jun, 21:45"),
            score = 6.5f,
            tags = listOf(
                LabelUM(text = TextReference.Str("Regulation"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "Bitwise has updated its Solana ETF, adding staking and setting a low management fee of 0.20%.",
            content = "Bitwise Asset Management has revised its filing to launch a Solana ETF, renaming it \"Bitwise Solana Staking ETF\" and setting an exceptionally low management fee of just 0.20%.\n\nThe update comes as the SEC prepares to review several Solana ETF applications.",
            sources = listOf(
                SourceUM(
                    id = 1,
                    title = "Deeper liquidity could drive crypto market beyond \$6T",
                    source = Source(
                        id = 11,
                        name = "Coin-telegraph",
                    ),
                    publishedAt = TextReference.Str("1h ago"),
                    url = "https://cointelegraph.com",
                    onClick = {},
                ),
                SourceUM(
                    id = 2,
                    title = "Top gainers and losers in crypto this week",
                    source = Source(
                        id = 10,
                        name = "Investing",
                    ),
                    publishedAt = TextReference.Str("2h ago"),
                    url = "https://investing.com",
                    onClick = {},
                ),
            ).toPersistentList(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 2,
            title = "Bitcoin ETFs log 4th straight day of inflows (+\$550M)",
            createdAt = TextReference.Str("20 Jun, 20:15"),
            score = 8.2f,
            tags = listOf(
                LabelUM(text = TextReference.Str("BTC"), size = LabelSize.BIG),
                LabelUM(text = TextReference.Str("ETF"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "Bitcoin spot ETFs recorded fourth consecutive day of positive inflows.",
            content = "Bitcoin spot ETFs continue their impressive streak with \$550 million in net positive inflows.\n\nBlackRock's IBIT led with \$250M.",
            sources = listOf(
                SourceUM(
                    id = 3,
                    title = "Bitcoin ETFs see massive inflows",
                    source = Source(
                        id = 12,
                        name = "Bloomberg",
                    ),
                    publishedAt = TextReference.Str("3h ago"),
                    url = "https://bloomberg.com",
                    onClick = {},
                ),
            ).toPersistentList(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 3,
            title = "Ethereum network upgrade scheduled for Q2 2025",
            createdAt = TextReference.Str("20 Jun, 18:30"),
            score = 7.8f,
            tags = listOf(
                LabelUM(text = TextReference.Str("ETH"), size = LabelSize.BIG),
                LabelUM(text = TextReference.Str("Technology"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "Ethereum developers announced major network upgrade.",
            content = "The Ethereum Foundation announced a significant upgrade for Q2 2025.\n\nKey improvements include EVM enhancements.",
            sources = listOf(
                SourceUM(
                    id = 4,
                    title = "Ethereum core devs announce upgrade",
                    source = Source(
                        id = 15,
                        name = "CoinDesk",
                    ),
                    publishedAt = TextReference.Str("5h ago"),
                    url = "https://coindesk.com",
                    onClick = {},
                ),
            ).toPersistentList(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 4,
            title = "Solana surpasses Ethereum in daily transaction volume",
            createdAt = TextReference.Str("20 Jun, 16:00"),
            score = 9.1f,
            tags = listOf(
                LabelUM(text = TextReference.Str("SOL"), size = LabelSize.BIG),
                LabelUM(text = TextReference.Str("Market"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "Solana achieved new milestone processing more daily transactions than Ethereum.",
            content = "Solana processed over 50 million transactions in a single day.\n\nDriven by DeFi and NFT activity.",
            sources = listOf(
                SourceUM(
                    id = 5,
                    title = "Solana transactions hit record",
                    source = Source(
                        id = 18,
                        name = "Times",
                    ),
                    publishedAt = TextReference.Str("7h ago"),
                    url = "https://theblock.co",
                    onClick = {},
                ),
            ).toPersistentList(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 5,
            title = "DeFi protocol launches innovative yield farming strategy",
            createdAt = TextReference.Str("20 Jun, 14:20"),
            score = 6.9f,
            tags = listOf(
                LabelUM(text = TextReference.Str("DeFi"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "New DeFi protocol introduced innovative yield farming approach.",
            content = "A newly launched protocol unveiled innovative yield farming mechanism.\n\nAPY rates range from 15% to 30%.",
            sources = persistentListOf(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 6,
            title = "Crypto regulation bill advances in US Senate",
            createdAt = TextReference.Str("20 Jun, 12:45"),
            score = 8.7f,
            tags = listOf(
                LabelUM(text = TextReference.Str("Regulation"), size = LabelSize.BIG),
                LabelUM(text = TextReference.Str("USA"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "Comprehensive cryptocurrency regulation bill passed Senate Banking Committee.",
            content = "The US Senate Banking Committee advanced landmark crypto regulation bill.\n\nKey provisions include asset definitions.",
            sources = persistentListOf(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 7,
            title = "Major bank announces crypto custody services",
            createdAt = TextReference.Str("20 Jun, 10:30"),
            score = 7.3f,
            tags = listOf(
                LabelUM(text = TextReference.Str("Adoption"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "World's largest bank announced cryptocurrency custody services.",
            content = "Major financial institution announced comprehensive crypto custody services.\n\nSupporting Bitcoin and Ethereum initially.",
            sources = persistentListOf(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 8,
            title = "NFT marketplace reports 300% increase in trading volume",
            createdAt = TextReference.Str("20 Jun, 08:15"),
            score = 5.8f,
            tags = listOf(
                LabelUM(text = TextReference.Str("NFT"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "Leading NFT marketplace experienced dramatic surge in trading activity.",
            content = "Prominent NFT marketplace reported 300% increase in trading volume.\n\nNew features include lower fees.",
            sources = persistentListOf(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 9,
            title = "Layer 2 solution achieves 100,000 TPS milestone",
            createdAt = TextReference.Str("19 Jun, 22:00"),
            score = 8.5f,
            tags = listOf(
                LabelUM(text = TextReference.Str("Technology"), size = LabelSize.BIG),
                LabelUM(text = TextReference.Str("L2"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "New Layer 2 scaling solution achieved 100,000 TPS in testing.",
            content = "Layer 2 solution processed 100,000 transactions per second.\n\nUsing zero-knowledge proof technology.",
            sources = persistentListOf(),
            newsUrl = "",
        ),
        ArticleUM(
            id = 10,
            title = "Stablecoin market cap reaches new all-time high",
            createdAt = TextReference.Str("19 Jun, 19:30"),
            score = 7.6f,
            tags = listOf(
                LabelUM(text = TextReference.Str("Stablecoins"), size = LabelSize.BIG),
                LabelUM(text = TextReference.Str("Market"), size = LabelSize.BIG),
            ).toPersistentList(),
            shortContent = "Total stablecoin market capitalization surpassed \$180 billion.",
            content = "Stablecoin market cap reached \$180 billion all-time high.\n\nDriven by DeFi activity and institutional adoption.",
            sources = persistentListOf(),
            newsUrl = "",
        ),
    ).toPersistentList()
}