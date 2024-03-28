package com.tangem.features.send.impl.presentation.ui.amount

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SendAmountContent(
    amountState: SendStates.AmountState?,
    isBalanceHiding: Boolean,
    clickIntents: SendClickIntents,
) {
    if (amountState == null) return
    LazyColumn(
        modifier = Modifier
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .background(TangemTheme.colors.background.tertiary),
    ) {
        amountField(amountState = amountState, isBalanceHiding = isBalanceHiding)
        buttons(amountState.segmentedButtonConfig, clickIntents)
        notifications(amountState.notifications)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.notifications(configs: ImmutableList<SendNotification>, modifier: Modifier = Modifier) {
    items(
        items = configs,
        key = { it::class.java },
        contentType = { it::class.java },
        itemContent = {
            val bottomPadding = if (it == configs.last()) TangemTheme.dimens.spacing12 else TangemTheme.dimens.spacing0
            Notification(
                config = it.config,
                modifier = modifier
                    .animateItemPlacement()
                    .padding(
                        top = TangemTheme.dimens.spacing12,
                        bottom = bottomPadding,
                    ),
                containerColor = when (it) {
                    is SendNotification.Warning.NetworkFeeUnreachable -> TangemTheme.colors.background.action
                    else -> TangemTheme.colors.button.disabled
                },
            )
        },
    )
}
