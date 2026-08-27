package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.entity.EarnOpportunitiesUM
import com.tangem.features.foryou.impl.entity.asSingleForYouGroup
import com.tangem.features.foryou.impl.model.converter.EarnOpportunities
import com.tangem.features.foryou.impl.model.converter.FOR_YOU_TOP_EARN_TOKENS_COUNT
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

/**
 * Earn-opportunities state for a portfolio with nothing earn-eligible: suggests the top
 * [FOR_YOU_TOP_EARN_TOKENS_COUNT] earn tokens, headed by the best (first) suggestion's yearly rate
 * ([EarnOpportunitiesUM.Content.potentialReward]) and its reward type, APR/APY
 * ([EarnOpportunitiesUM.Content.potentialRewardType]).
 */
internal class ForYouEarnOpportunitiesNoTokensConverter(
    private val topEarnTokens: EarnTopToken?,
    private val onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit,
    private val onAllEarnTokensClick: () -> Unit,
) : Converter<List<EarnOpportunities>, EarnOpportunitiesUM> {

    override fun convert(value: List<EarnOpportunities>): EarnOpportunitiesUM {
        val topEarnTokenList = topEarnTokens?.getOrNull()
            ?.take(FOR_YOU_TOP_EARN_TOKENS_COUNT)

        val topEarnToken = topEarnTokenList?.firstOrNull()?.earnToken
        val topEarnApy = topEarnToken?.apy?.parseBigDecimalOrNull()

        val rowConverter = ForYouEarnOpportunitiesTopTokenRowConverter(onTokenClick = onTokenClick)

        return EarnOpportunitiesUM.Content(
            tokenList = topEarnTokenList
                ?.map(rowConverter::convert)
                .orEmpty()
                .toPersistentList()
                .asSingleForYouGroup(),
            subtitleRes = R.string.for_you_earn_opportunities_no_available_tokens,
            potentialReward = stringReference(topEarnApy.format { percent() }),
            potentialRewardType = topEarnToken?.rewardType?.name?.let(::stringReference),
            onAllEarnTokensClick = onAllEarnTokensClick,
        )
    }
}