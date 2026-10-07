package com.tangem.features.promobanners.impl.campaigns.v2.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class PromoCampaignStatusUM(
    val variant: TangemStatusScreen.Variant,
    val icon: TangemStatusIcon.Kind,
    val title: TextReference,
    val description: TextReference?,
    val details: Details?,
    val buttonText: TextReference,
    val onButtonClick: () -> Unit,
) {

    @Immutable
    sealed interface Details {

        data class EnrolledWallets(val wallets: ImmutableList<UserWalletItemUM>) : Details

        /** [tokenIcon] is `null` when the registered payout isn't the chosen token. */
        data class Recipient(
            val walletName: TextReference,
            val tokenIcon: CurrencyIconState?,
            val address: String,
        ) : Details
    }
}