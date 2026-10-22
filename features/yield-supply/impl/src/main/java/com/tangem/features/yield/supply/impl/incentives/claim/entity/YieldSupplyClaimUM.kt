package com.tangem.features.yield.supply.impl.incentives.claim.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * Fees are `null` while they load, which the fee rows render as a shimmer.
 *
 * @property isClaimInProgress the claim is being signed; the sheet neither reloads nor closes meanwhile
 */
@Immutable
internal data class YieldSupplyClaimUM(
    val total: TextReference,
    val groups: ImmutableList<RewardGroupUM>,
    val serviceFee: TextReference?,
    val networkFee: TextReference?,
    val networkFeeCurrency: TextReference?,
    val isServiceFeeVisible: Boolean,
    val notification: NotificationUM?,
    val isClaimEnabled: Boolean,
    val isClaimInProgress: Boolean,
    val isHoldToConfirmEnabled: Boolean,
) {

    /** @property storage the user's account; set only for the owner destination */
    @Immutable
    internal data class RewardGroupUM(
        val title: TextReference,
        val rewards: ImmutableList<RewardUM>,
        val storage: AccountTitleUM.Account? = null,
    )

    @Immutable
    internal data class RewardUM(
        val icon: CurrencyIconState,
        val symbol: String,
        val networkName: String,
        val amount: TextReference,
        val fiatAmount: TextReference?,
    )
}