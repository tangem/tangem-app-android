package com.tangem.features.foryou.impl.ui.preview

import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouWalletGroupUM
import com.tangem.features.foryou.impl.entity.ForYouWalletHeaderUM
import com.tangem.features.foryou.impl.entity.asSingleForYouGroup
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal object ForYouEarnOpportunitiesPreviewData {

    private const val TOTAL_POTENTIAL_REWARD = "\$32.14/year"
    private const val TOP_EARN_APY = "4.5%/year"

    /** Tokens with earn opportunities: subtitle with the inline reward badge + flat token rows. */
    val tokensRewards = EarnOpportunitiesUM.Content(
        subtitleRes = R.string.for_you_earn_opportunities_tokens_rewards,
        potentialReward = stringReference(TOTAL_POTENTIAL_REWARD),
        potentialRewardType = null,
        tokenList = persistentListOf(
            earnTokenRow(
                id = "token_0",
                name = "Ethereum",
                network = "Ethereum network",
                topEnd = "+ \$24.16/year",
                bottomEnd = "4,5%",
            ),
            earnTokenRow(
                id = "token_1",
                name = "Solana",
                network = "Solana network",
                topEnd = "+ \$7.98/year",
                bottomEnd = "6,2%",
            ),
        ).asSingleForYouGroup(),
        onAllEarnTokensClick = {},
    )

    /** Earn-eligible holdings spread across two wallets: one grouped section per wallet with a header. */
    val groupedByWallet = EarnOpportunitiesUM.Content(
        subtitleRes = R.string.for_you_earn_opportunities_tokens_rewards,
        potentialReward = stringReference(TOTAL_POTENTIAL_REWARD),
        potentialRewardType = null,
        tokenList = persistentListOf(
            ForYouWalletGroupUM(
                header = walletHeader(id = "wallet_0", name = "Tangem wallet"),
                items = persistentListOf(
                    earnTokenRow(
                        id = "wallet_0_main",
                        name = "Main Account",
                        network = "7 tokens",
                        topEnd = "+ \$394/year",
                        bottomEnd = "",
                    ),
                ),
            ),
            ForYouWalletGroupUM(
                header = walletHeader(id = "wallet_1", name = "Tangem wallet 2.0"),
                items = persistentListOf(
                    earnTokenRow(
                        id = "wallet_1_solana",
                        name = "Solana",
                        network = "Solana network",
                        topEnd = "+ \$41.83/year",
                        bottomEnd = "APY 19.44%",
                    ),
                    earnTokenRow(
                        id = "wallet_1_tether",
                        name = "Tether",
                        network = "Solana network",
                        topEnd = "+ \$20/year",
                        bottomEnd = "APY 3.44%",
                    ),
                ),
            ),
        ),
        onAllEarnTokensClick = {},
    )

    /** No earnable tokens in the portfolio: top earn tokens teaser. */
    val noAvailableTokens = EarnOpportunitiesUM.Content(
        subtitleRes = R.string.for_you_earn_opportunities_no_available_tokens,
        potentialReward = stringReference(TOP_EARN_APY),
        potentialRewardType = stringReference("APY"),
        tokenList = persistentListOf(
            earnTokenRow(
                id = "top_token_0",
                name = "TON",
                network = "TON network",
                topEnd = "APY 4,5",
                bottomEnd = "Staking",
            ),
        ).asSingleForYouGroup(),
        onAllEarnTokensClick = {},
    )

    /** Every earnable token is already earning: plain subtitle, no reward badge, no rows. */
    val allTokensActive = EarnOpportunitiesUM.Content(
        subtitleRes = R.string.for_you_earn_opportunities_all_tokens_active,
        potentialReward = null,
        potentialRewardType = null,
        tokenList = persistentListOf(),
        onAllEarnTokensClick = {},
    )

    val loading = EarnOpportunitiesUM.Loading(
        tokenList = List(size = 4) { index ->
            ForYouTokenListItemUM(
                tokenRowUM = TangemTokenRowUM.Loading(id = index.toString()),
                tokenList = persistentListOf(),
                isExpanded = false,
                isExpandable = false,
                segmentColor = null,
            )
        }.toPersistentList().asSingleForYouGroup(),
    )

    private fun walletHeader(id: String, name: String): ForYouWalletHeaderUM = ForYouWalletHeaderUM(
        id = id,
        name = stringReference(name),
        deviceIcon = DeviceIconUM.Stub(cardsCount = 1),
    )

    private fun earnTokenRow(
        id: String,
        name: String,
        network: String,
        topEnd: String,
        bottomEnd: String,
    ): ForYouTokenListItemUM {
        return ForYouTokenListItemUM(
            tokenRowUM = TangemTokenRowUM.Content(
                id = id,
                headIconUM = TangemIconUM.Currency(CurrencyIconState.Loading),
                titleUM = TangemTokenRowUM.TitleUM.Content(text = stringReference(name)),
                subtitleUM = TangemTokenRowUM.SubtitleUM.Content(text = stringReference(network)),
                topEndContentUM = TangemTokenRowUM.EndContentUM.Content(text = stringReference(topEnd)),
                bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(text = stringReference(bottomEnd)),
                onItemClick = null,
                onItemLongClick = null,
            ),
            tokenList = persistentListOf(),
            isExpanded = false,
            isExpandable = false,
            segmentColor = null,
        )
    }
}