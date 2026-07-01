package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.ButtonState
import com.tangem.features.tangempay.entity.TangemPayTxHistoryDetailsUMV2
import com.tangem.features.tangempay.entity.TransactionLabelUM
import com.tangem.features.tangempay.entity.TransactionStateType

@Suppress("LongMethod")
@Composable
internal fun TangemPayTxHistoryDetailsContentV2(state: TangemPayTxHistoryDetailsUMV2) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.dismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                title = state.title,
                subtitle = state.subtitle,
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = state.dismiss,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = TangemTheme.dimens2.x4)
                    .padding(bottom = TangemTheme.dimens2.x4),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TransactionIcon(
                    iconState = state.iconState,
                    modifier = Modifier.padding(top = TangemTheme.dimens2.x12),
                )
                Text(
                    modifier = Modifier.padding(top = TangemTheme.dimens2.x6),
                    text = state.transactionAmount.orMaskWithStars(state.isBalanceHidden),
                    style = TangemTheme.typography3.display.medium,
                    color = TangemTheme.colors3.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = TangemTheme.typography3.body.medium.fontSize,
                        maxFontSize = TangemTheme.typography3.display.medium.fontSize,
                    ),
                )
                TransactionSecondaryLine(
                    state = state,
                    modifier = Modifier.padding(top = TangemTheme.dimens2.x1),
                )
                if (state.label != null) {
                    TransactionLabel(
                        label = state.label,
                        modifier = Modifier
                            .padding(top = TangemTheme.dimens2.x12)
                            .fillMaxWidth(),
                    )
                }
                TransactionDetailsBlock(
                    state = state,
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens2.x4, bottom = TangemTheme.dimens2.x2)
                        .fillMaxWidth(),
                )
                TangemButton(
                    modifier = Modifier
                        .padding(vertical = TangemTheme.dimens2.x4)
                        .fillMaxWidth(),
                    text = state.buttonState.text,
                    onClick = state.buttonState.onClick,
                    size = TangemButton.Size.X12,
                )
            }
        },
    )
}

@Composable
private fun TransactionIcon(iconState: TangemIconUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x20)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.opaque.primary),
        contentAlignment = Alignment.Center,
    ) {
        TangemIcon(
            tangemIconUM = iconState,
            modifier = Modifier.size(
                if (iconState is TangemIconUM.Icon) {
                    TangemTheme.dimens2.x12
                } else {
                    TangemTheme.dimens2.x20
                },
            ),
        )
    }
}

@Composable
private fun TransactionSecondaryLine(state: TangemPayTxHistoryDetailsUMV2, modifier: Modifier = Modifier) {
    val secondaryText = when {
        state.localTransactionText != null -> {
            buildString {
                append(state.localTransactionText.orMaskWithStars(state.isBalanceHidden))
                append(" · ")
                append(state.transactionTitle.resolveReference())
            }
        }
        else -> state.transactionTitle.resolveReference()
    }
    Text(
        modifier = modifier,
        text = secondaryText,
        style = TangemTheme.typography3.subheading.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun TransactionLabel(label: TransactionLabelUM, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = when (label.transactionStateType) {
        TransactionStateType.Completed,
        TransactionStateType.Reversed,
        -> {
            TangemTheme.colors3.bg.status.successSubtle to TangemTheme.colors3.text.status.success
        }
        TransactionStateType.InProgress -> {
            TangemTheme.colors3.bg.status.infoSubtle to TangemTheme.colors3.text.status.info
        }
        TransactionStateType.Rejected -> {
            TangemTheme.colors3.bg.status.errorSubtle to TangemTheme.colors3.text.status.error
        }
    }

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(
                vertical = TangemTheme.dimens2.x3,
                horizontal = TangemTheme.dimens2.x4,
            ),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
        ) {
            Text(
                text = label.title.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = textColor,
            )

            label.subtitle?.let { text ->
                Text(
                    text = text.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = textColor,
                )
            }
        }

        SpacerW(TangemTheme.dimens2.x1)

        TangemIcon(
            tangemIconUM = label.icon,
            modifier = Modifier.size(TangemTheme.dimens2.x5),
        )
    }
}

@Composable
private fun TransactionDetailsBlock(state: TangemPayTxHistoryDetailsUMV2, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemRow(
            divider = state.mcc != null,
            contentLead = TangemRowContentLead.Start,
            titleSlot = {
                TangemRowText(
                    text = resourceReference(R.string.tangem_pay_transaction_details_category),
                    role = TangemRowTextRole.Title,
                )
            },
            valueSlot = {
                TangemRowText(
                    text = state.transactionCategory,
                    role = TangemRowTextRole.Value,
                )
            },
        )
        if (state.mcc != null) {
            TangemRow(
                contentLead = TangemRowContentLead.Start,
                titleSlot = {
                    TangemRowText(
                        text = resourceReference(R.string.tangem_pay_transaction_details_mcc),
                        role = TangemRowTextRole.Title,
                    )
                },
                valueSlot = {
                    TangemRowText(
                        text = state.mcc,
                        role = TangemRowTextRole.Value,
                    )
                },
            )
        }
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayTxHistoryDetailsContentPreview(
    @PreviewParameter(TangemPayTxHistoryDetailsUMProviderV2::class) state: TangemPayTxHistoryDetailsUMV2,
) {
    TangemThemePreviewRedesign {
        TangemPayTxHistoryDetailsContentV2(state = state)
    }
}

private class TangemPayTxHistoryDetailsUMProviderV2 :
    CollectionPreviewParameterProvider<TangemPayTxHistoryDetailsUMV2>(
        listOf(
            TangemPayTxHistoryDetailsUMV2(
                isBalanceHidden = true,
                title = resourceReference(R.string.tangem_pay_purchase),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_category_24),
                transactionTitle = stringReference("Starbucks"),
                transactionCategory = stringReference("Food and drinks"),
                mcc = stringReference("5814"),
                transactionAmount = "-$5.86",
                localTransactionText = null,
                label = TransactionLabelUM(
                    transactionStateType = TransactionStateType.InProgress,
                    icon = TangemIconUM.Icon(iconRes = com.tangem.core.ui.R.drawable.ic_clock_24),
                    title = resourceReference(R.string.tangem_pay_status_pending),
                ),
                buttonState = ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
                dismiss = {},
            ),
            TangemPayTxHistoryDetailsUMV2(
                isBalanceHidden = true,
                title = resourceReference(R.string.tangem_pay_purchase),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_category_24),
                transactionTitle = stringReference("NuCaloric"),
                transactionCategory = stringReference("Groceries"),
                mcc = stringReference("0000"),
                transactionAmount = "-$820.52",
                localTransactionText = "-€696,52",
                label = TransactionLabelUM(
                    transactionStateType = TransactionStateType.Rejected,
                    icon = TangemIconUM.Icon(iconRes = R.drawable.ic_token_info_24),
                    title = resourceReference(R.string.tangem_pay_status_declined),
                    subtitle = stringReference("Reason: account credit limit exceeded"),
                ),
                buttonState = ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
                dismiss = {},
            ),
            TangemPayTxHistoryDetailsUMV2(
                isBalanceHidden = true,
                title = resourceReference(R.string.tangem_pay_purchase),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_category_24),
                transactionTitle = stringReference("Starbucks"),
                transactionCategory = stringReference("Food and drinks"),
                mcc = null,
                transactionAmount = "-$5.86",
                localTransactionText = "€ 5.36",
                label = TransactionLabelUM(
                    transactionStateType = TransactionStateType.Completed,
                    icon = TangemIconUM.Empty,
                    title = resourceReference(R.string.tangem_pay_status_completed),
                ),
                buttonState = ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
                dismiss = {},
            ),
            TangemPayTxHistoryDetailsUMV2(
                isBalanceHidden = false,
                title = resourceReference(R.string.tangem_pay_fee_title),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_percent_24),
                transactionTitle = stringReference("Service fees"),
                transactionCategory = stringReference("Service fees"),
                mcc = null,
                transactionAmount = "-$5.86",
                localTransactionText = null,
                label = TransactionLabelUM(
                    transactionStateType = TransactionStateType.Completed,
                    icon = TangemIconUM.Icon(iconRes = R.drawable.ic_token_info_24),
                    title = resourceReference(R.string.tangem_pay_fee_title),
                    subtitle = resourceReference(R.string.tangem_pay_transaction_fee_notification_text),
                ),
                buttonState = ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
                dismiss = {},
            ),
            TangemPayTxHistoryDetailsUMV2(
                isBalanceHidden = false,
                title = resourceReference(R.string.tangem_pay_deposit),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(imageVector = Icons.ic_arrow_down_24),
                transactionTitle = resourceReference(R.string.common_transfer),
                transactionCategory = resourceReference(R.string.common_transfer),
                mcc = null,
                transactionAmount = "+$20",
                localTransactionText = null,
                label = null,
                buttonState = ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
                dismiss = {},
            ),
        ),
    )