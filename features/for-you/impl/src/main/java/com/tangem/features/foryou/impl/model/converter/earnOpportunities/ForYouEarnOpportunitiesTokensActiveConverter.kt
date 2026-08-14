package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.tangem.common.ui.R
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.asSingleForYouGroup
import com.tangem.features.foryou.impl.model.converter.EarnOpportunities
import com.tangem.features.foryou.impl.model.converter.FOR_YOU_TOP_EARN_TOKENS_COUNT
import com.tangem.features.foryou.impl.model.converter.forYouEarnAssetKey
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

/**
 * Earn-opportunities state for a portfolio where every earn-eligible token is already active:
 * suggests the top earn tokens the user is not earning on yet (the ones already active are filtered out).
 *
 * Matching is per asset **and** network (see [forYouEarnAssetKey]), so an asset active on one network
 * can still be suggested on another. Active tokens are filtered out before the
 * [FOR_YOU_TOP_EARN_TOKENS_COUNT] cap, so exclusions don't shrink the suggestion list while more
 * candidates remain in the batch.
 */
internal class ForYouEarnOpportunitiesTokensActiveConverter(
    private val topEarnTokens: EarnTopToken?,
    private val onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit,
    private val onAllEarnTokensClick: () -> Unit,
) : Converter<List<EarnOpportunities>, EarnOpportunitiesUM> {

    override fun convert(value: List<EarnOpportunities>): EarnOpportunitiesUM {
        val activeAssetKeys = value
            .flatMap { opportunities -> opportunities.earnCurrencies.keys }
            .map { status -> status.currency.forYouEarnAssetKey() }
            .toSet()

        val rowConverter = ForYouEarnOpportunitiesTopTokenRowConverter(onTokenClick)

        return EarnOpportunitiesUM.Content(
            tokenList = topEarnTokens?.getOrNull()
                ?.filterNot { topToken -> topToken.cryptoCurrency.forYouEarnAssetKey() in activeAssetKeys }
                ?.take(FOR_YOU_TOP_EARN_TOKENS_COUNT)
                ?.map(rowConverter::convert)
                .orEmpty()
                .toPersistentList()
                .asSingleForYouGroup(),
            subtitleRes = R.string.for_you_earn_opportunities_all_tokens_active,
            potentialReward = null,
            potentialRewardType = null,
            onAllEarnTokensClick = onAllEarnTokensClick,
        )
    }
}