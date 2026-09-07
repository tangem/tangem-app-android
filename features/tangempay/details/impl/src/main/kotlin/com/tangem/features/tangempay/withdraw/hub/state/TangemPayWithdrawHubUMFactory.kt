package com.tangem.features.tangempay.withdraw.hub.state

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.features.tangempay.details.impl.R

internal class TangemPayWithdrawHubUMFactory(
    private val onBackClick: () -> Unit,
    private val onWithinPortfolioClick: () -> Unit,
) {

    fun create(): TangemPayWithdrawHubUM = TangemPayWithdrawHubUM(
        swapSection = TangemPayWithdrawHubUM.SwapSectionUM(
            withinPortfolio = TangemPayWithdrawHubUM.ActionCardUM(
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_swap_horizontal_20,
                    tintReference = { TangemTheme.colors3.icon.staticDark },
                ),
                title = resourceReference(R.string.tangempay_withdraw_hub_within_portfolio),
                onClick = onWithinPortfolioClick,
            ),
        ),
        onBackClick = onBackClick,
    )
}