package com.tangem.features.earn.product.api.product

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.api.overview.EarnProductRate
import kotlinx.coroutines.flow.Flow

/** A rate and a click route without screens of its own: Yield Mode and StakeKit staking. */
interface EarnRateSource {

    val id: EarnProductId

    val rank: Int

    val badgeAccent: EarnBadgeAccent

    /** No entry for a currency means the source has no rate for it. The first frame comes from cache, without waiting. */
    fun rates(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnRateEntry>>
}

/** @property route where a click leads if this source wins: the screen opened before Earn, or `null` to open Earn */
data class EarnRateEntry(
    val rate: EarnProductRate,
    val isActive: Boolean,
    val source: StatusSource,
    val route: Route? = null,
)