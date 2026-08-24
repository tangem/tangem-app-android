package com.tangem.features.tangempay.txhistory.details

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.CircleShimmer
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
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R

@Suppress("LongMethod")
@Composable
internal fun TangemPayTxHistoryDetailsContent(state: TangemPayTxHistoryDetailsUM) {
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
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens2.x6)
                        .testTag(TangemPayTestTags.TRANSACTION_DETAILS_AMOUNT),
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
            .size(80.dp)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.opaque.primary),
        contentAlignment = Alignment.Center,
    ) {
        when (iconState) {
            is TangemIconUM.Url -> MerchantIcon(url = iconState.url, fallbackRes = iconState.fallbackRes)
            else -> TangemIcon(
                tangemIconUM = iconState,
                modifier = Modifier.size(
                    if (iconState is TangemIconUM.Icon) {
                        48.dp
                    } else {
                        80.dp
                    },
                ),
            )
        }
    }
}

@Composable
private fun MerchantIcon(url: String?, @DrawableRes fallbackRes: Int) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(url)
            .crossfade(enable = true)
            .allowHardware(enable = false)
            .build(),
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        loading = { CircleShimmer(Modifier.size(80.dp)) },
        // A logo fills the circle, but the fallback glyph is a 24dp icon: drawn at the logo's size it
        // renders as an oversized untinted shape, so it keeps the plain-icon size and tint instead.
        // The Box absorbs the min constraints the image propagates, which would otherwise stretch it.
        error = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = ImageVector.vectorResource(fallbackRes),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = TangemTheme.colors3.icon.secondary,
                )
            }
        },
    )
}

@Composable
private fun TransactionSecondaryLine(state: TangemPayTxHistoryDetailsUM, modifier: Modifier = Modifier) {
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
private fun TransactionDetailsBlock(state: TangemPayTxHistoryDetailsUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        when (val detail = state.detail) {
            null -> Unit
            TransactionDetailUM.Loading -> DetailRowShimmer()
            is TransactionDetailUM.Content -> DetailValueRow(
                title = resourceReference(R.string.tangempay_common_card),
                value = detail.cardNumber,
                subvalue = detail.cardName,
            )
            is TransactionDetailUM.Error -> DetailRowError(
                title = resourceReference(R.string.tangempay_common_card),
                onRefreshClick = detail.onRefreshClick,
            )
        }
        when (val cashback = state.cashbackDetail) {
            null -> Unit
            CashbackDetailUM.Loading -> DetailRowShimmer()
            CashbackDetailUM.AwaitingCalculation -> CashbackRowPlaceholder()
            is CashbackDetailUM.Content -> DetailValueRow(
                title = resourceReference(R.string.tangempay_cashback_title),
                value = cashback.value,
                subvalue = cashback.subvalue,
            )
            is CashbackDetailUM.Error -> DetailRowError(
                title = resourceReference(R.string.tangempay_cashback_title),
                onRefreshClick = cashback.onRefreshClick,
            )
        }
        TangemRow(
            divider = state.mcc != null,
            contentLead = TangemRowContentLead.End,
            titleSlot = {
                TangemRowText(
                    text = resourceReference(R.string.tangem_pay_transaction_details_category),
                    role = TangemRowTextRole.Title,
                )
            },
            valueSlot = { DetailRowValue(text = state.transactionCategory) },
        )
        if (state.mcc != null) {
            TangemRow(
                contentLead = TangemRowContentLead.End,
                titleSlot = {
                    TangemRowText(
                        text = resourceReference(R.string.tangem_pay_transaction_details_mcc),
                        role = TangemRowTextRole.Title,
                    )
                },
                valueSlot = { DetailRowValue(text = state.mcc) },
            )
        }
    }
}

/** A value-and-optional-subvalue row shared by the Card and Cashback rows. */
@Composable
private fun DetailValueRow(
    title: TextReference,
    value: TextReference?,
    subvalue: TextReference?,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier,
        divider = true,
        contentLead = TangemRowContentLead.End,
        titleSlot = {
            TangemRowText(text = title, role = TangemRowTextRole.Title)
        },
        valueSlot = if (value != null) {
            { DetailRowValue(text = value) }
        } else {
            null
        },
        subvalueSlot = if (subvalue != null) {
            { DetailRowSubvalue(text = subvalue) }
        } else {
            null
        },
    )
}

@Composable
private fun DetailRowShimmer(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        divider = true,
        contentLead = TangemRowContentLead.End,
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.tangempay_common_card),
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = {
            TangemShimmer(
                style = TangemTheme.typography3.body.medium,
                textAlign = TextAlign.End,
            )
        },
        subvalueSlot = {
            TangemShimmer(
                style = TangemTheme.typography3.caption.medium,
                textAlign = TextAlign.End,
            )
        },
    )
}

/** Cashback not yet calculated by the backend — placeholder shimmer in place of the value. */
@Composable
private fun CashbackRowPlaceholder(modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        divider = true,
        contentLead = TangemRowContentLead.End,
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.tangempay_cashback_title),
                role = TangemRowTextRole.Title,
            )
        },
        valueSlot = {
            TangemShimmer(
                modifier = Modifier.size(width = 56.dp, height = 20.dp),
                radius = 999.dp,
            )
        },
    )
}

@Composable
private fun DetailRowError(title: TextReference, onRefreshClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        divider = true,
        contentLead = TangemRowContentLead.End,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(text = title, role = TangemRowTextRole.Title)
        },
        valueSlot = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onRefreshClick)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = resourceReference(R.string.tangempay_common_error_loading).resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.status.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.ic_arrow_refresh_20,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.status.error,
                )
            }
        },
    )
}

@Composable
private fun DetailRowValue(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text.resolveReference(),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.body.medium,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun DetailRowSubvalue(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = text.resolveReference(),
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayTxHistoryDetailsContentPreview(
    @PreviewParameter(TangemPayTxHistoryDetailsUMProvider::class) state: TangemPayTxHistoryDetailsUM,
) {
    TangemThemePreviewRedesign {
        TangemPayTxHistoryDetailsContent(state = state)
    }
}

private class TangemPayTxHistoryDetailsUMProvider :
    CollectionPreviewParameterProvider<TangemPayTxHistoryDetailsUM>(
        listOf(
            TangemPayTxHistoryDetailsUM(
                isBalanceHidden = true,
                title = resourceReference(R.string.tangem_pay_purchase),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_category_24),
                transactionTitle = stringReference("Starbucks"),
                detail = TransactionDetailUM.Content(
                    cardNumber = stringReference("*9092"),
                    cardName = stringReference("Basic card"),
                ),
                cashbackDetail = CashbackDetailUM.Content(value = stringReference("+$0.75"), subvalue = null),
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
            TangemPayTxHistoryDetailsUM(
                isBalanceHidden = true,
                title = resourceReference(R.string.tangem_pay_purchase),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_category_24),
                transactionTitle = stringReference("NuCaloric"),
                detail = TransactionDetailUM.Content(
                    cardNumber = stringReference("*9092"),
                    cardName = stringReference("Basic card"),
                ),
                cashbackDetail = CashbackDetailUM.Content(
                    value = stringReference("No cashback"),
                    subvalue = stringReference("MCC excluded"),
                ),
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
            TangemPayTxHistoryDetailsUM(
                isBalanceHidden = true,
                title = resourceReference(R.string.tangem_pay_purchase),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_category_24),
                transactionTitle = stringReference("Starbucks"),
                detail = TransactionDetailUM.Content(
                    cardNumber = stringReference("*9092"),
                    cardName = stringReference("Basic card"),
                ),
                cashbackDetail = CashbackDetailUM.Content(
                    value = stringReference("-$0.50"),
                    subvalue = stringReference("return of purchase"),
                ),
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
            TangemPayTxHistoryDetailsUM(
                isBalanceHidden = false,
                title = resourceReference(R.string.tangem_pay_fee_title),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_percent_24),
                transactionTitle = stringReference("Service fees"),
                detail = null,
                cashbackDetail = null,
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
            TangemPayTxHistoryDetailsUM(
                isBalanceHidden = false,
                title = resourceReference(R.string.tangem_pay_deposit),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(imageVector = Icons.ic_arrow_down_24),
                transactionTitle = resourceReference(R.string.common_transfer),
                detail = null,
                cashbackDetail = null,
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
            TangemPayTxHistoryDetailsUM(
                isBalanceHidden = false,
                title = resourceReference(R.string.tangem_pay_purchase),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_category_24),
                transactionTitle = stringReference("Starbucks"),
                detail = TransactionDetailUM.Loading,
                cashbackDetail = CashbackDetailUM.Loading,
                transactionCategory = stringReference("Food and drinks"),
                mcc = stringReference("5814"),
                transactionAmount = "-$5.86",
                localTransactionText = null,
                label = TransactionLabelUM(
                    transactionStateType = TransactionStateType.Completed,
                    icon = TangemIconUM.Icon(iconRes = R.drawable.ic_token_info_24),
                    title = resourceReference(R.string.tangem_pay_status_completed),
                ),
                buttonState = ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
                dismiss = {},
            ),
            TangemPayTxHistoryDetailsUM(
                isBalanceHidden = false,
                title = resourceReference(R.string.tangem_pay_purchase),
                subtitle = stringReference("12 June 2026, 12:40"),
                iconState = TangemIconUM.Icon(iconRes = R.drawable.ic_category_24),
                transactionTitle = stringReference("Starbucks"),
                detail = TransactionDetailUM.Error(onRefreshClick = {}),
                cashbackDetail = CashbackDetailUM.Error(onRefreshClick = {}),
                transactionCategory = stringReference("Food and drinks"),
                mcc = stringReference("5814"),
                transactionAmount = "-$5.86",
                localTransactionText = null,
                label = TransactionLabelUM(
                    transactionStateType = TransactionStateType.Completed,
                    icon = TangemIconUM.Icon(iconRes = R.drawable.ic_token_info_24),
                    title = resourceReference(R.string.tangem_pay_status_completed),
                ),
                buttonState = ButtonState(
                    text = resourceReference(R.string.tangem_pay_get_help),
                    onClick = {},
                ),
                dismiss = {},
            ),
        ),
    )