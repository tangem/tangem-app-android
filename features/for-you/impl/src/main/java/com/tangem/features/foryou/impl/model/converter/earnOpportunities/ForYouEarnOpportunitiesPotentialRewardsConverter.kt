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
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouWalletGroupUM
import com.tangem.features.foryou.impl.entity.ForYouWalletHeaderUM
import com.tangem.features.foryou.impl.model.converter.EarnOpportunities
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

/**
 * Earn-opportunities state for a portfolio with tokens that could earn but don't yet: renders the
 * user's earn-eligible holdings with their projected yearly rewards, headed by the total across
 * accounts ([EarnOpportunitiesUM.Content.potentialReward]).
 *
 * With accounts mode on, each account becomes one expandable row (children delegated to
 * [ForYouEarnOpportunitiesTokenRowConverter], expansion keyed by account id); with it off, the
 * tokens are rendered as flat non-expandable rows.
 */
@Suppress("LongParameterList")
internal class ForYouEarnOpportunitiesPotentialRewardsConverter(
    private val appCurrency: AppCurrency,
    private val isAccountsModeEnabled: Boolean,
    private val expandClick: (assetId: String) -> Unit,
    private val onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit,
    private val onAllEarnTokensClick: () -> Unit,
    private val walletHeaders: Map<UserWalletId, ForYouWalletHeaderUM>,
    private val isBalanceHidden: Boolean = false,
) : Converter<List<EarnOpportunities>, EarnOpportunitiesUM> {

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
                }.orMaskWithStars(isBalanceHidden),
            ),
        )

        val byWallet = value.groupBy { it.userWalletId }
        val hasMultipleWallets = byWallet.size > 1
        val groups = byWallet.map { (userWalletId, walletEarnData) ->
            ForYouWalletGroupUM(
                header = if (hasMultipleWallets) walletHeaders[userWalletId] else null,
                items = walletEarnData.flatMap(::createWalletItems).toPersistentList(),
            )
        }.toPersistentList()

        return EarnOpportunitiesUM.Content(
            tokenList = groups,
            subtitleRes = R.string.for_you_earn_opportunities_tokens_rewards,
            potentialReward = totalPotentialRewardText,
            potentialRewardType = null,
            onAllEarnTokensClick = onAllEarnTokensClick,
        )
    }

    private fun createWalletItems(earnData: EarnOpportunities): List<ForYouTokenListItemUM> {
        // Each account keeps its own wallet id (the selection can span wallets), so build a row
        // converter per account to route token clicks to the wallet that owns them.
        val rowConverter = ForYouEarnOpportunitiesTokenRowConverter(
            appCurrency = appCurrency,
            userWalletId = earnData.userWalletId,
            onTokenClick = onTokenClick,
            isBalanceHidden = isBalanceHidden,
        )
        return if (isAccountsModeEnabled) {
            listOf(
                ForYouTokenListItemUM(
                    tokenRowUM = createAssetRow(
                        account = earnData.account,
                        potentialReward = earnData.accountPotentialReward,
                        tokenCount = earnData.earnCurrencies.size,
                    ),
                    tokenList = rowConverter.convertList(earnData.earnCurrencies.toList())
                        .toPersistentList(),
                    isExpanded = false,
                    isExpandable = true,
                    segmentColor = null,
                ),
            )
        } else {
            earnData.earnCurrencies.map { token ->
                ForYouTokenListItemUM(
                    tokenRowUM = rowConverter.convert(token.toPair()),
                    tokenList = persistentListOf(),
                    isExpanded = false,
                    isExpandable = false,
                    segmentColor = null,
                )
            }
        }
    }

    private fun createAssetRow(
        account: Account.CryptoPortfolio,
        potentialReward: BigDecimal?,
        tokenCount: Int,
    ): TangemTokenRowUM {
        return TangemTokenRowUM.Content(
            id = account.accountId.value,
            headIconUM = TangemIconUM.Currency(
                currencyIconState = AccountIconItemStateConverter(size = AccountIconSize.Default)
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
                    if (!isBalanceHidden) {
                        stringReference(StringsSigns.PLUS + StringsSigns.WHITE_SPACE)
                    } else {
                        TextReference.EMPTY
                    },
                    resourceReference(
                        R.string.for_you_earn_per_year,
                        wrappedList(
                            potentialReward.format {
                                fiat(
                                    fiatCurrencySymbol = appCurrency.symbol,
                                    fiatCurrencyCode = appCurrency.code,
                                )
                            }.orMaskWithStars(isBalanceHidden),
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