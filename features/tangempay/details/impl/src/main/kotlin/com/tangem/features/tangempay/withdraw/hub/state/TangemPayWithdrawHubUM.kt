package com.tangem.features.tangempay.withdraw.hub.state

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

internal data class TangemPayWithdrawHubUM(
    val swapSection: SwapSectionUM,
    val onBackClick: () -> Unit,
) {

    data class SwapSectionUM(
        val withinPortfolio: ActionCardUM,
    )

    data class ActionCardUM(
        val icon: TangemIconUM,
        val title: TextReference,
        val onClick: () -> Unit,
    )
}