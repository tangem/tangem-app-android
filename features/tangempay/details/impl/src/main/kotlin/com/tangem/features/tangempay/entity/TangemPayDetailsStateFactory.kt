package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.persistentListOf

internal class TangemPayDetailsStateFactory(
    private val onBack: () -> Unit,
    private val onRefresh: (ShowRefreshState) -> Unit,
    private val onReceive: () -> Unit,
    private val onClickChangePin: () -> Unit,
    private val onClickFreezeCard: () -> Unit,
) {

    fun getInitialState() = TangemPayDetailsUM(
        topBarConfig = TangemPayDetailsTopBarConfig(
            onBackClick = onBack,
            items = persistentListOf(
                TangemDropdownMenuItem(
                    title = TextReference.Res(R.string.tangempay_card_details_change_pin),
                    textColorProvider = { TangemTheme.colors.text.primary1 },
                    onClick = onClickChangePin,
                ),
                TangemDropdownMenuItem(
                    title = TextReference.Res(R.string.tangempay_card_details_freeze_card),
                    textColorProvider = { TangemTheme.colors.text.primary1 },
                    onClick = onClickFreezeCard,
                ),
            ),
        ),
        pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = onRefresh),
        balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(
            actionButtons = persistentListOf(
                ActionButtonConfig(
                    text = resourceReference(id = R.string.common_receive),
                    iconResId = R.drawable.ic_arrow_down_24,
                    onClick = onReceive,
                ),
            ),
        ),
        isBalanceHidden = false,
    )
}