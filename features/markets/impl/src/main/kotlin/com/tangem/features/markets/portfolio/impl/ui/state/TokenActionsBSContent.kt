package com.tangem.features.markets.portfolio.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.markets.impl.R
import kotlinx.collections.immutable.ImmutableList

internal data class TokenActionsBSContent(
    val title: String,
    val actions: ImmutableList<Action>,
    val onActionClick: (Action) -> Unit,
) : TangemBottomSheetConfigContent {

    @Immutable
    enum class Action(
        val text: TextReference,
    ) {
        CopyAddress(text = resourceReference(R.string.common_copy_address)),
        Receive(text = resourceReference(R.string.common_receive)),
        Sell(text = resourceReference(R.string.common_sell)),
        Buy(text = resourceReference(R.string.common_buy)),
        Send(text = resourceReference(R.string.common_send)),
        Exchange(text = resourceReference(R.string.common_exchange)),
        Stake(text = resourceReference(R.string.common_stake)),
    }
}