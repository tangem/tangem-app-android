package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.tangem.common.ui.R
import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.model.converter.EarnOpportunities
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class ForYouEarnOpportunitiesPotentialRewardsConverter(
    private val appCurrency: AppCurrency,
    private val isAccountsModeEnabled: Boolean,
    private val expandedAssetIds: Set<String>,
    private val expandClick: (assetId: String) -> Unit,
) : Converter<List<EarnOpportunities>, EarnOpportunitiesUM> {

    private val rowConverter = ForYouEarnOpportunitiesTokenRowConverter(appCurrency = appCurrency)

    override fun convert(value: List<EarnOpportunities>): EarnOpportunitiesUM {
        val totalPotentialReward = value.sumOf { it.accountPotentialReward }
        val totalPotentialRewardText = resourceReference(
            R.string.for_you_earn_per_year,
            wrappedList(
                totalPotentialReward.format {
                    fiat(
                        fiatCurrencySymbol = appCurrency.symbol,
                        fiatCurrencyCode = appCurrency.code,
                    )
                },
            ),
        )

        return EarnOpportunitiesUM.Content(
            tokenList = value.flatMap { earnData ->
                if (isAccountsModeEnabled) {
                    listOf(
                        ForYouTokenListItemUM(
                            tokenRowUM = createAssetRow(
                                account = earnData.account,
                                potentialReward = earnData.accountPotentialReward,
                                tokenCount = earnData.earnCurrencues.size,
                            ),
                            tokenList = rowConverter.convertList(earnData.earnCurrencues.toList())
                                .toPersistentList(),
                            isExpanded = earnData.account.accountId.value in expandedAssetIds,
                            isExpandable = true,
                        ),
                    )
                } else {
                    earnData.earnCurrencues.map { token ->
                        ForYouTokenListItemUM(
                            tokenRowUM = rowConverter.convert(token.toPair()),
                            tokenList = persistentListOf(),
                            isExpanded = false,
                            isExpandable = false,
                        )
                    }
                }
            }.toPersistentList(),
            subtitleRes = R.string.for_you_earn_opportunities_tokens_rewards,
            potentialReward = totalPotentialRewardText,
            potentialRewardType = null,
        )
    }

    private fun createAssetRow(
        account: Account.CryptoPortfolio,
        potentialReward: BigDecimal?,
        tokenCount: Int,
    ): TangemTokenRowUM {
        return TangemTokenRowUM.Content(
            id = account.accountId.value,
            headIconUM = TangemIconUM.Currency(
                currencyIconState = AccountIconItemStateConverter(size = AccountIconSize.RedesignedDefault)
                    .convert(account),
            ),
            titleUM = TangemTokenRowUM.TitleUM.Content(
                text = account.accountName.toUM().value,
            ),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                text = pluralReference(
                    R.plurals.common_tokens_count,
                    count = tokenCount,
                    formatArgs = wrappedList(tokenCount),
                ),
            ),
            topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = combinedReference(
                    stringReference(StringsSigns.PLUS),
                    resourceReference(
                        R.string.for_you_earn_per_year,
                        wrappedList(
                            potentialReward.format {
                                fiat(
                                    fiatCurrencySymbol = appCurrency.symbol,
                                    fiatCurrencyCode = appCurrency.code,
                                )
                            },
                        ),
                    ),
                ),
            ),
            bottomEndContentUM = TangemTokenRowUM.EndContentUM.Empty,
            onItemClick = { expandClick(account.accountId.value) },
            onItemLongClick = null,
        )
    }
}