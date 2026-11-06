package com.tangem.features.earn.flow.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.amountScreen.AmountScreenContent
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.notifications
import com.tangem.core.ui.components.buttons.predefined.PredefinedPercentButtonsRow
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.earn.api.ui.EarnRowUM
import com.tangem.features.earn.flow.impl.ui.state.EarnAmountUM
import com.tangem.features.earn.flow.impl.ui.state.EarnConfirmUM
import com.tangem.features.earn.flow.impl.ui.state.EarnResultUM
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorBlockComponent
import kotlinx.collections.immutable.ImmutableList

/* Plain screens until the layouts from the design replace them; the UM and the model stay. */

@Composable
internal fun EarnFlowLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
        contentAlignment = Alignment.Center,
    ) {
        TangemLoader()
    }
}

@Composable
internal fun EarnAmountContent(
    state: EarnAmountUM,
    feeBlock: FeeSelectorBlockComponent?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EarnFlowScaffold(
        title = state.title,
        onBack = null,
        onClose = onClose,
        bottom = {
            PredefinedPercentButtonsRow(items = state.percents)
            EarnFlowButton(state.primary)
        },
        modifier = modifier,
    ) {
        AmountScreenContent(
            amountState = state.amountState,
            clickIntents = state.clickIntents,
            modifier = Modifier.weight(1f),
            extraContent = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    feeBlock?.Content(Modifier.fillMaxWidth())
                    EarnNotifications(state.notifications)
                }
            },
        )
    }
}

@Composable
internal fun EarnConfirmContent(
    state: EarnConfirmUM,
    title: TextReference,
    feeBlock: FeeSelectorBlockComponent?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EarnFlowScaffold(
        title = title,
        onBack = onBack,
        onClose = null,
        bottom = {
            state.stepLabel?.let { EarnSecondaryText(it) }
            EarnFlowButton(state.primary)
        },
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "amount") {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.amountFiat.resolveReference(),
                        style = TangemTheme.typography3.heading.medium,
                        color = TangemTheme.colors3.text.primary,
                    )
                    EarnSecondaryText(state.amountCrypto)
                }
            }
            items(items = state.rows, key = { it.label.hashCode() }) { row -> EarnRow(row) }
            item(key = "fee") { feeBlock?.Content(Modifier.fillMaxWidth()) }
            state.notice?.let { notice -> item(key = "notice") { EarnSecondaryText(notice) } }
            notifications(state.notifications)
            state.legal?.let { legal -> item(key = "legal") { EarnSecondaryText(legal) } }
        }
    }
}

@Composable
internal fun EarnResultContent(state: EarnResultUM, modifier: Modifier = Modifier) {
    TangemStatusScreen(
        variant = state.variant,
        title = state.title,
        description = state.subtitle,
        modifier = modifier,
        extraBottom = state.chip?.let { chip -> { EarnSecondaryText(chip) } },
        details = { state.rows.forEach { row -> EarnRow(row) } },
        footer = {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.secondary?.let { EarnFlowButton(it) }
                state.primary?.let { EarnFlowButton(it) }
            }
        },
    )
}

@Composable
private fun EarnFlowScaffold(
    title: TextReference,
    onBack: (() -> Unit)?,
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    bottom: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .navigationBarsPadding()
            .imePadding(),
    ) {
        TangemTopNavigation(title = title, onBack = onBack, onClose = onClose)
        content()
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bottom()
        }
    }
}

@Composable
private fun EarnNotifications(notifications: ImmutableList<NotificationUM>) {
    notifications.forEach { notification -> Notification(config = notification.config) }
}

@Composable
private fun EarnRow(row: EarnRowUM) {
    Column(modifier = Modifier.fillMaxWidth()) {
        EarnSecondaryText(row.label)
        Text(
            text = row.value.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun EarnSecondaryText(text: TextReference) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

/** Only `text`, `isEnabled`, `isDimmed`, `isLoading` and `onClick` of [TangemButtonUM] apply: the DS3 look is fixed here. */
@Composable
private fun EarnFlowButton(button: TangemButtonUM) {
    TangemButton(
        modifier = Modifier.fillMaxWidth(),
        variant = if (button.type == TangemButtonType.Primary) {
            TangemButton.Variant.Primary
        } else {
            TangemButton.Variant.Secondary
        },
        size = TangemButton.Size.X14,
        text = button.text,
        isEnabled = button.isEnabled,
        isDimmed = button.isDimmed,
        isLoading = button.isLoading,
        onClick = button.onClick,
    )
}