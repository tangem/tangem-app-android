package com.tangem.features.foryou.impl.model.converter

import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal
import java.math.RoundingMode

/** Number of suggested top-earn tokens shown in the earn-opportunities block. */
internal const val FOR_YOU_TOP_EARN_TOKENS_COUNT = 5

/**
 * Batch size for the top-earn-tokens request. Only the first batch is ever fetched (the section shows
 * at most [FOR_YOU_TOP_EARN_TOKENS_COUNT] rows), but it is requested larger so that filtering out
 * already-active tokens still leaves enough candidates to fill the list.
 */
internal const val TOP_EARN_TOKENS_BATCH_SIZE = 30

/** Divisor converting backend percent values (5.5) to fractions (0.055). */
internal val PERCENT_BASE = BigDecimal("100")

/**
 * Cross-network grouping key for the portfolio review: the same asset on different networks (e.g. USDC
 * on Solana and Ethereum) shares its `rawCurrencyId`, so they group under a single item. Custom tokens
 * have no raw id and fall back to their unique currency id, staying in their own group.
 */
internal fun CryptoCurrencyStatus.forYouGroupKey(): String = currency.id.rawCurrencyId?.value ?: currency.id.value

/**
 * Matching key between a portfolio currency and a top-earn suggestion: the same asset
 * (`rawCurrencyId`, falling back to the unique id for custom tokens) on the same network.
 */
internal fun CryptoCurrency.forYouEarnAssetKey(): Pair<String, String> =
    (id.rawCurrencyId?.value ?: id.value) to network.rawId

/**
 * Computes this fiat amount as a share of [totalFiatBalance]. Returns `null` when the share cannot be
 * computed (no amount, or a zero total / amount).
 */
internal fun BigDecimal?.toForYouPercent(totalFiatBalance: BigDecimal): BigDecimal? {
    if (this == null || totalFiatBalance.isZero() || isZero()) return null
    return divide(totalFiatBalance, RoundingMode.HALF_UP)
}

// TODO For You: replace this placeholder with the real price-change badge once the design is wired.
internal fun forYouPlaceholderBadge(): TangemBadgeUM = TangemBadgeUM(
    text = stringReference("Positive"),
    size = TangemBadgeSize.X4,
    type = TangemBadgeType.Tinted,
    color = TangemBadgeColor.Green,
)

/**
 * Earn rate resolved for a portfolio currency.
 *
 * @property isActive whether the user already earns on the token (active yield supply or stake)
 * @property apy rate as a fraction (0.05 = 5%)
 * @property potentialRewards projected yearly reward in fiat (`fiatAmount * apy`), `null` when unknown
 * @property type which earn product the rate belongs to; passed to the click callback so the model
 * can open the matching earn screen
 */
internal data class EarnApyInfo(
    val isActive: Boolean,
    val apy: BigDecimal?,
    val potentialRewards: BigDecimal?,
    val type: ForYouEarnOpportunitiesType,
)

/**
 * Earn-eligible currencies of one account with their resolved rates.
 *
 * @property accountPotentialReward sum of [EarnApyInfo.potentialRewards] over [earnCurrencies];
 * accounts are ordered by it, descending
 */
internal data class EarnOpportunities(
    val account: Account.CryptoPortfolio,
    val earnCurrencies: Map<CryptoCurrencyStatus, EarnApyInfo>,
    val accountPotentialReward: BigDecimal,
)