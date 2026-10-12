package com.tangem.features.yield.supply.impl.incentives.claim.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * Claim sheet contents.
 *
 * A fee is `null` while it is still being estimated, which is what the fee rows render a shimmer for.
 *
 * @property isClaimInProgress a claim is being prepared and signed; the sheet neither reloads nor closes meanwhile.
 */
@Immutable
internal data class YieldSupplyClaimUM(
    val total: TextReference,
    val groups: ImmutableList<RewardGroupUM>,
    val serviceFee: TextReference?,
    val networkFee: TextReference?,
    val networkFeeCurrency: TextReference?,
    val isServiceFeeVisible: Boolean,
    val warning: Warning?,
    val isClaimEnabled: Boolean,
    val isClaimInProgress: Boolean,
) {

    /**
     * Rewards sharing one backend destination, headed by what that destination means for the user.
     *
     * @property storage account the rewards are credited to, shown only where the destination is the owner's wallet
     */
    @Immutable
    internal data class RewardGroupUM(
        val title: TextReference,
        val rewards: ImmutableList<RewardUM>,
        val storage: StorageUM? = null,
    )

    /** Who holds what a group pays out, as the header names it. */
    @Immutable
    internal data class StorageUM(val name: TextReference, val icon: AccountIconUM)

    /** @property fiatAmount null for rewards outside the claim total — no USD value is invented for them. */
    @Immutable
    internal data class RewardUM(
        val icon: CurrencyIconState,
        val symbol: String,
        val networkName: String,
        val amount: TextReference,
        val fiatAmount: TextReference?,
    )

    internal enum class Warning {
        /** The service did not confirm a claim for these rewards, so the sheet offers a retry and signs nothing. */
        RewardsUnavailable,
    }
}