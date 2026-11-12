package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.api.overview.EarnHeadline
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.api.overview.EarnOverviews
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import java.math.BigDecimal

internal val TEST_EARN_BADGE_TEXT: TextReference = stringReference("APY 5.50%")

internal fun earnHeadline(
    isActive: Boolean = false,
    badgeAccent: EarnBadgeAccent = EarnBadgeAccent.Success,
    fraction: String = "0.055",
) = EarnHeadline(
    rate = EarnProductRate(fraction = BigDecimal(fraction), type = EarnRateType.APY),
    isActive = isActive,
    badge = TEST_EARN_BADGE_TEXT,
    badgeAccent = badgeAccent,
    route = null,
)

internal fun earnOverviewsOf(currency: CryptoCurrency, headline: EarnHeadline?) = EarnOverviews(
    byCurrency = mapOf(currency.id to EarnOverview(headline = headline, banner = null)),
    source = StatusSource.ACTUAL,
)