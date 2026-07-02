package com.tangem.features.foryou.impl.model.converter

import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Formatting helpers shared by the For You portfolio-review converters.
 *
 * Kept null-safe so that non-[CryptoCurrencyStatus.Loaded] states (which carry no fiat amount) degrade
 * to a dash / empty instead of throwing.
 */

/**
 * Cross-network grouping key for the portfolio review: the same asset on different networks (e.g. USDC
 * on Solana and Ethereum) shares its `rawCurrencyId`, so they group under a single item. Custom tokens
 * have no raw id and fall back to their unique currency id, staying in their own group.
 */
internal fun CryptoCurrencyStatus.forYouGroupKey(): String = currency.id.rawCurrencyId?.value ?: currency.id.value

/** Formats a (possibly null) fiat amount in [appCurrency]; a `null` amount renders as a dash. */
internal fun BigDecimal?.toForYouFiatText(appCurrency: AppCurrency): TextReference = stringReference(
    format { fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol) },
)

/**
 * Formats this fiat amount as a share of [totalFiatBalance]. Returns [TextReference.EMPTY] when the
 * share cannot be computed (no amount, or a zero total / amount).
 */
internal fun BigDecimal?.toForYouPercentText(totalFiatBalance: BigDecimal): TextReference {
    if (this == null || totalFiatBalance.isZero() || isZero()) return TextReference.EMPTY
    return stringReference(divide(totalFiatBalance, RoundingMode.HALF_UP).format { percent() })
}

// TODO For You: replace this placeholder with the real price-change badge once the design is wired.
internal fun forYouPlaceholderBadge(): TangemBadgeUM = TangemBadgeUM(
    text = stringReference("Positive"),
    size = TangemBadgeSize.X4,
    type = TangemBadgeType.Tinted,
    color = TangemBadgeColor.Green,
)