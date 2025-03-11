package com.tangem.features.markets.portfolio.impl.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.markets.impl.R
import kotlinx.collections.immutable.ImmutableList

internal data class TokenActionsBSContentUM(
    val title: String,
    val actions: ImmutableList<Action>,
    val onActionClick: (Action) -> Unit,
) : TangemBottomSheetConfigContent {

    @Immutable
    enum class Action(
        val text: TextReference,
        @DrawableRes val iconRes: Int,
    ) {
        CopyAddress(
            text = resourceReference(R.string.common_copy_address),
            iconRes = R.drawable.ic_copy_24,
        ),
        Send(
            text = resourceReference(R.string.common_send),
            iconRes = R.drawable.ic_arrow_up_24,
        ),
        Receive(
            text = resourceReference(R.string.common_receive),
            iconRes = R.drawable.ic_arrow_down_24,
        ),
        Buy(
            text = resourceReference(R.string.common_buy),
            iconRes = R.drawable.ic_plus_24,
        ),
        Sell(
            text = resourceReference(R.string.common_sell),
            iconRes = R.drawable.ic_currency_24,
        ),
        Exchange(
            text = resourceReference(R.string.common_exchange),
            iconRes = R.drawable.ic_exchange_horizontal_24,
        ),
        Stake(
            text = resourceReference(R.string.common_stake),
            iconRes = R.drawable.ic_staking_24,
        ),
        ;

        val order: Int = ordinal
    }
}