package com.tangem.features.earn.api.overview

import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface EarnOverviewSupplier {

    operator fun invoke(userWalletId: UserWalletId): Flow<EarnOverviews>
}

interface EarnOverviewFetcher : FlowFetcher<EarnOverviewFetcher.Params> {

    data class Params(val userWalletId: UserWalletId, val currency: CryptoCurrency)
}

/**
 * @property source `CACHE`: products are still answering; `ACTUAL`: all answered; `ONLY_CACHE`: some failed or
 * returned cache
 */
data class EarnOverviews(
    val byCurrency: Map<CryptoCurrency.ID, EarnOverview>,
    val source: StatusSource,
) {

    val isComplete: Boolean
        get() = source == StatusSource.ACTUAL

    operator fun get(id: CryptoCurrency.ID): EarnOverview? = byCurrency[id]

    companion object {

        /** `ACTUAL`, not `CACHE`: a host must not take an overview of an unsuitable wallet for "still loading". */
        val Empty = EarnOverviews(byCurrency = emptyMap(), source = StatusSource.ACTUAL)
    }
}

data class EarnOverview(
    val headline: EarnHeadline?,
    val banner: EarnBannerOverview?,
)

data class EarnBannerOverview(
    val rate: EarnProductRate?,
    val activeCount: Int,
    val totalPosition: BigDecimal?,
)

data class EarnHeadline(
    val rate: EarnProductRate,
    val isActive: Boolean,
    val badge: TextReference,
    val route: Route?,
)

/** @property fraction `0.05` is 5% */
data class EarnProductRate(val fraction: BigDecimal, val type: EarnRateType)

enum class EarnRateType { APY, APR }

/** How a host paints the badge of an active product. Names no product: a host must not know which one it shows. */
enum class EarnBadgeAccent { Info, Success }