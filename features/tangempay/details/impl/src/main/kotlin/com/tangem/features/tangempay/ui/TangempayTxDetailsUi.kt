package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayTxHistoryDetailsUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
@Composable
internal fun TangemPayTxHistoryDetailsContent(state: TangemPayTxHistoryDetailsUM) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.dismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.dismiss,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = state.title,
                endIconRes = R.drawable.ic_close_24,
                onEndClick = state.dismiss,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(88.dp),
                iconState = state.iconState,
            )
            Text(
                modifier = Modifier.padding(top = 32.dp),
                text = state.transactionTitle.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = state.transactionSubtitle.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.tertiary,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = state.transactionAmount.orMaskWithStars(state.isBalanceHidden),
                style = TangemTheme.typography.head,
                color = state.transactionAmountColor.resolveReference(),
            )
            state.localTransactionText?.let { localTransaction ->
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = localTransaction,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
            state.labelState?.let { Label(state = state.labelState, modifier = Modifier.padding(top = 12.dp)) }
            SpacerH32()
            state.notification?.let {
                Notification(
                    config = state.notification,
                    titleColor = TangemTheme.colors.text.tertiary,
                    iconTint = TangemTheme.colors.icon.secondary,
                )
            }
            ButtonsContainer(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                buttons = state.buttons,
            )
        }
    }
}

@Composable
private fun ButtonsContainer(
    buttons: ImmutableList<TangemPayTxHistoryDetailsUM.ButtonState>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        buttons.fastForEach { buttonState ->
            if (buttonState.startIcon != null) {
                SecondaryButtonIconStart(
                    modifier = Modifier.fillMaxWidth(),
                    text = buttonState.text.resolveReference(),
                    iconResId = buttonState.startIcon.resId,
                    onClick = buttonState.onClick,
                )
            } else {
                SecondaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = buttonState.text.resolveReference(),
                    onClick = buttonState.onClick,
                )
            }
        }
    }
}

@Composable
private fun Icon(iconState: ImageReference, modifier: Modifier = Modifier) {
    when (iconState) {
        is ImageReference.Res -> LocalStaticIcon(modifier = modifier, id = iconState.resId, iconSize = 40.dp)
        is ImageReference.Url -> RemoteIcon(modifier = modifier, url = iconState.url)
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayTxHistoryDetailsContentPreview(
    @PreviewParameter(TangemPayTxHistoryDetailsUMProvider::class) state: TangemPayTxHistoryDetailsUM,
) {
    TangemThemePreview {
        TangemPayTxHistoryDetailsContent(state = state)
    }
}

private class TangemPayTxHistoryDetailsUMProvider : CollectionPreviewParameterProvider<TangemPayTxHistoryDetailsUM>(
    listOf(
        TangemPayTxHistoryDetailsUM(
            isBalanceHidden = true,
            title = stringReference("12 June • 12:40"),
            iconState = ImageReference.Res(R.drawable.ic_category_24),
            transactionTitle = stringReference("Starbucks"),
            transactionSubtitle = stringReference("Food and drinks"),
            transactionAmount = "-$5.86",
            transactionAmountColor = themedColor { TangemTheme.colors.text.primary1 },
            localTransactionText = null,
            labelState = LabelUM(
                text = resourceReference(R.string.tangem_pay_status_pending),
                style = LabelStyle.REGULAR,
                icon = R.drawable.ic_clock_24,
            ),
            notification = null,
            buttons = persistentListOf(
                TangemPayTxHistoryDetailsUM.ButtonState(
                    text = resourceReference(R.string.tangem_pay_dispute),
                    onClick = {},
                ),
            ),
            dismiss = {},
        ),
        TangemPayTxHistoryDetailsUM(
            isBalanceHidden = true,
            title = stringReference("12 June • 12:40"),
            iconState = ImageReference.Res(R.drawable.ic_category_24),
            transactionTitle = stringReference("Starbucks"),
            transactionSubtitle = stringReference("Food and drinks"),
            transactionAmount = "-$5.86",
            transactionAmountColor = themedColor { TangemTheme.colors.text.warning },
            localTransactionText = null,
            labelState = LabelUM(
                text = resourceReference(R.string.tangem_pay_status_declined),
                style = LabelStyle.WARNING,
            ),
            notification = NotificationConfig(
                title = stringReference("The bank rejected this transaction request."),
                subtitle = TextReference.EMPTY,
                iconResId = R.drawable.ic_token_info_24,
            ),
            buttons = persistentListOf(
                TangemPayTxHistoryDetailsUM.ButtonState(
                    text = resourceReference(R.string.tangem_pay_dispute),
                    onClick = {},
                ),
            ),
            dismiss = {},
        ),
        TangemPayTxHistoryDetailsUM(
            isBalanceHidden = false,
            title = stringReference("12 June • 12:40"),
            iconState = ImageReference.Res(R.drawable.ic_category_24),
            transactionTitle = stringReference("Starbucks"),
            transactionSubtitle = stringReference("Food and drinks"),
            transactionAmount = "-$5.86",
            transactionAmountColor = themedColor { TangemTheme.colors.text.primary1 },
            localTransactionText = "€ 5.36",
            labelState = LabelUM(
                text = resourceReference(R.string.tangem_pay_status_completed),
                style = LabelStyle.ACCENT,
            ),
            notification = null,
            buttons = persistentListOf(
                TangemPayTxHistoryDetailsUM.ButtonState(
                    text = resourceReference(R.string.tangem_pay_dispute),
                    onClick = {},
                ),
            ),
            dismiss = {},
        ),
        TangemPayTxHistoryDetailsUM(
            isBalanceHidden = false,
            title = stringReference("12 June • 12:40"),
            iconState = ImageReference.Res(R.drawable.ic_percent_24),
            transactionTitle = stringReference("Fee"),
            transactionSubtitle = stringReference("Service fee"),
            transactionAmount = "-$5.86",
            transactionAmountColor = themedColor { TangemTheme.colors.text.primary1 },
            localTransactionText = null,
            labelState = null,
            notification = NotificationConfig(
                title = stringReference("This fee goes to cover the cost of handling your transfer."),
                subtitle = TextReference.EMPTY,
                iconResId = R.drawable.ic_token_info_24,
            ),
            buttons = persistentListOf(
                TangemPayTxHistoryDetailsUM.ButtonState(
                    text = resourceReference(R.string.tangem_pay_dispute),
                    onClick = {},
                ),
            ),
            dismiss = {},
        ),
        TangemPayTxHistoryDetailsUM(
            isBalanceHidden = false,
            title = stringReference("12 June • 12:40"),
            iconState = ImageReference.Res(R.drawable.ic_arrow_down_24),
            transactionTitle = stringReference("Deposit"),
            transactionSubtitle = stringReference("Transfers"),
            transactionAmount = "+$20",
            transactionAmountColor = themedColor { TangemTheme.colors.text.accent },
            localTransactionText = null,
            labelState = null,
            notification = null,
            buttons = persistentListOf(
                TangemPayTxHistoryDetailsUM.ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
            ),
            dismiss = {},
        ),
        TangemPayTxHistoryDetailsUM(
            isBalanceHidden = false,
            title = stringReference("12 June • 12:40"),
            iconState = ImageReference.Res(R.drawable.ic_arrow_up_24),
            transactionTitle = stringReference("Withdrawal"),
            transactionSubtitle = stringReference("Transfers"),
            transactionAmount = "-$5.86",
            transactionAmountColor = themedColor { TangemTheme.colors.text.primary1 },
            localTransactionText = null,
            labelState = null,
            notification = null,
            buttons = persistentListOf(
                TangemPayTxHistoryDetailsUM.ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
            ),
            dismiss = {},
        ),
    ),
)