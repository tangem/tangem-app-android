package com.tangem.features.earn.flow.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.earn.flow.impl.ui.state.EarnAmountUM
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent

@Composable
internal fun EarnAmountContent(
    state: EarnAmountUM,
    feeBlock: FeeSelectorBlockComponent?,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EarnFlowScaffold(
        topBar = { TopBar(state = state, onBack = onBack, onClose = onClose) },
        bottom = {
            if (state.percents.isNotEmpty()) EarnPercentButtons(items = state.percents)
            EarnFlowButton(button = state.primary, modifier = Modifier.fillMaxWidth())
        },
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "amount") {
                EarnAmountCard(
                    amountState = state.amountState,
                    clickIntents = state.clickIntents,
                    isSourceShown = !state.isFromPosition,
                )
            }
            item(key = "fee") { feeBlock?.Content(Modifier.fillMaxWidth()) }
            state.notice?.let { notice -> item(key = "notice") { EarnNotice(notice) } }
            if (state.notifications.isNotEmpty()) {
                item(key = "notifications") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { EarnNotifications(state.notifications) }
                }
            }
        }
    }
}

/** A deposit has "How it works" on the start; a withdrawal leads with back and moves it next to close. */
@Composable
private fun TopBar(state: EarnAmountUM, onBack: () -> Unit, onClose: () -> Unit) {
    val infoButton: (@Composable () -> Unit)? = state.onHowItWorksClick?.let { onClick ->
        {
            TangemButton(
                variant = TangemButton.Variant.Material,
                size = TangemButton.Size.X11,
                iconStart = TangemIconUM.Icon(Icons.ic_info_20),
                onClick = onClick,
            )
        }
    }
    TangemTopNavigation(
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        startButton = if (state.isFromPosition) {
            { TangemButton.Back(onClick = onBack) }
        } else {
            infoButton
        },
        endButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isFromPosition) infoButton?.invoke()
                TangemButton.Close(onClick = onClose)
            }
        },
        contentColumn = { TangemNavigationText(text = state.title, role = TangemNavigationText.Role.Title) },
    )
}