package com.tangem.features.earn.impl.fake

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnBannerOverview
import com.tangem.features.earn.api.overview.EarnHeadline
import com.tangem.features.earn.api.overview.EarnOverview
import com.tangem.features.earn.api.overview.EarnOverviewSupplier
import com.tangem.features.earn.api.overview.EarnOverviews
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeEarnOverviewSupplier(
    private val overviews: EarnOverviews = fakeEarnOverviews(),
) : EarnOverviewSupplier {

    override fun invoke(userWalletId: UserWalletId): Flow<EarnOverviews> = flowOf(overviews)
}

fun fakeEarnOverviews(): EarnOverviews = EarnOverviews(
    byCurrency = mapOf(
        CryptoCurrency.ID.fromValue(FAKE_ETH_CURRENCY_ID) to EarnOverview(
            headline = EarnHeadline(
                rate = fakeRate(),
                isActive = false,
                badge = stringReference("APY 5.50%"),
                route = null,
            ),
            banner = EarnBannerOverview(rate = fakeRate(), activeCount = 0, totalPosition = null),
        ),
    ),
    source = StatusSource.ACTUAL,
)