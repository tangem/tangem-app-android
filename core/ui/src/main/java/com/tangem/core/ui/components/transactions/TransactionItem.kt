package com.tangem.core.ui.components.transactions

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Direction
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TransactionHistoryItemTestTags

@Composable
fun TransactionItem(state: TransactionItemUM, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    when (state) {
        is TransactionItemUM.Content -> ContentItem(
            state = state,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
        )
        is TransactionItemUM.Pill -> TransactionStatusPill(
            state = state,
            isBalanceHidden = isBalanceHidden,
            modifier = modifier,
        )
        is TransactionItemUM.Loading,
        is TransactionItemUM.Locked,
        -> Unit
    }
}

@Composable
private fun ContentItem(state: TransactionItemUM.Content, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = state.onClick)
            .testTag(TransactionHistoryItemTestTags.ITEM),
    ) {
        TangemRowContainer(
            contentPadding = PaddingValues(
                horizontal = TangemTheme.dimens2.x4,
                vertical = TangemTheme.dimens2.x3,
            ),
        ) {
            StatusCircle(
                iconRes = state.iconRes,
                status = state.status,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.HEAD)
                    .padding(end = TangemTheme.dimens2.x3)
                    .size(TangemTheme.dimens2.x10)
                    .testTag(TransactionHistoryItemTestTags.STATUS_PREFIX + state.status.testTagSuffix),
            )
            TitleText(
                title = state.title,
                status = state.status,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.START_TOP)
                    .testTag(TransactionHistoryItemTestTags.TITLE),
            )
            SubtitleText(
                subtitle = state.subtitle,
                status = state.status,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.START_BOTTOM)
                    .padding(top = TangemTheme.dimens2.x0_5),
            )
            AmountText(
                amount = state.amount,
                status = state.status,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.END_TOP)
                    .testTag(TransactionHistoryItemTestTags.AMOUNT),
            )
            CurrencyText(
                symbol = state.currencySymbol,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.END_BOTTOM)
                    .padding(top = TangemTheme.dimens2.x0_5)
                    .testTag(TransactionHistoryItemTestTags.CURRENCY),
            )
        }
        state.warning?.let { warning ->
            WarningLine(
                warning = warning,
                modifier = Modifier.padding(
                    start = TangemTheme.dimens2.x4,
                    end = TangemTheme.dimens2.x4,
                    bottom = TangemTheme.dimens2.x3,
                ),
            )
        }
    }
}

@Composable
private fun WarningLine(warning: TextReference, modifier: Modifier = Modifier) {
    val attention = TangemTheme.colors2.text.status.attention
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alert_triangle_20),
            contentDescription = null,
            tint = attention,
            modifier = Modifier.size(TangemTheme.dimens2.x5),
        )
        Text(
            text = warning.resolveReference(),
            color = attention,
            style = TangemTheme.typography2.captionMedium12,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// region Status circle

@Composable
private fun StatusCircle(iconRes: Int, status: Status, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = status.backgroundColor,
            shape = CircleShape,
        ),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = status.iconTint,
            modifier = Modifier
                .size(TangemTheme.dimens2.x5)
                .align(Alignment.Center),
        )
    }
}

private val Status.backgroundColor: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors2.markers.backgroundTintedGray
        is Status.Unconfirmed -> TangemTheme.colors2.markers.backgroundTintedBlue
        is Status.Failed -> TangemTheme.colors2.markers.backgroundTintedRed
    }

private val Status.iconTint: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors2.fill.neutral.primary
        is Status.Unconfirmed -> TangemTheme.colors2.markers.iconBlue
        is Status.Failed -> TangemTheme.colors2.markers.iconRed
    }

private val Status.testTagSuffix: String
    get() = when (this) {
        is Status.Confirmed -> "CONFIRMED"
        is Status.Unconfirmed -> "UNCONFIRMED"
        is Status.Failed -> "FAILED"
    }

// endregion

// region Title / Subtitle

@Composable
private fun TitleText(title: TextReference, status: Status, modifier: Modifier = Modifier) {
    Text(
        text = title.resolveReference(),
        color = status.titleColor,
        style = TangemTheme.typography2.bodyMedium16,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private val Status.titleColor: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors2.text.neutral.primary
        is Status.Unconfirmed -> TangemTheme.colors2.text.status.accent
        is Status.Failed -> TangemTheme.colors2.text.status.warning
    }

@Suppress("LongMethod")
@Composable
private fun SubtitleText(subtitle: ContentSubtitle, status: Status, modifier: Modifier = Modifier) {
    val textStyle = TangemTheme.typography2.captionMedium12
    val tertiary = TangemTheme.colors2.text.neutral.tertiary
    val primary = TangemTheme.colors2.text.neutral.primary
    val isFailed = status is Status.Failed
    when (subtitle) {
        is ContentSubtitle.Plain -> Text(
            text = subtitle.text.resolveReference(),
            color = tertiary,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        is ContentSubtitle.PlainAddress -> PlainAddressText(
            subtitle = subtitle,
            status = status,
            modifier = modifier,
        )
        is ContentSubtitle.ExternalAddress -> InlineImageSubtitle(
            template = stringResourceSafe(subtitle.direction.templateResId(), subtitle.briefAddress),
            color = tertiary,
            afterIconColor = if (isFailed) tertiary else primary,
            modifier = modifier,
        ) {
            IdentIcon(
                address = subtitle.rawAddress,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
        is ContentSubtitle.OwnAccount -> InlineImageSubtitle(
            template = stringResourceSafe(
                subtitle.direction.templateResId(),
                subtitle.accountName.resolveReference(),
            ),
            color = tertiary,
            afterIconColor = if (isFailed) tertiary else primary,
            modifier = modifier,
        ) {
            val backgroundColor = if (isFailed) {
                TangemTheme.colors2.graphic.neutral.quaternary
            } else {
                subtitle.iconBackgroundColor
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(TangemTheme.dimens2.x1))
                    .background(backgroundColor),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = subtitle.iconResId),
                    contentDescription = null,
                    tint = TangemTheme.colors.text.constantWhite,
                    modifier = Modifier.size(TangemTheme.dimens2.x2_5),
                )
            }
        }
        is ContentSubtitle.OwnWallet -> InlineImageSubtitle(
            template = stringResourceSafe(subtitle.direction.templateResId(), subtitle.walletName),
            color = tertiary,
            afterIconColor = primary,
            modifier = modifier,
        ) {
            TangemDeviceIcon(
                state = subtitle.deviceIconUM,
                modifier = Modifier.fillMaxSize(),
            )
        }
        is ContentSubtitle.Asset -> InlineImageSubtitle(
            template = stringResourceSafe(subtitle.direction.templateResId(), subtitle.symbol),
            color = tertiary,
            afterIconColor = if (isFailed) tertiary else primary,
            modifier = modifier,
        ) {
            subtitle.icon?.let { iconState ->
                CurrencyIcon(
                    state = iconState,
                    shouldDisplayNetwork = false,
                    withFixedSize = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PlainAddressText(subtitle: ContentSubtitle.PlainAddress, status: Status, modifier: Modifier = Modifier) {
    val full = subtitle.text.resolveReference()
    val highlightColor = if (status is Status.Failed) {
        TangemTheme.colors2.text.neutral.tertiary
    } else {
        TangemTheme.colors2.text.neutral.primary
    }
    val text = remember(full, subtitle.highlight, highlightColor) {
        buildAnnotatedString {
            append(full)
            val start = full.lastIndexOf(subtitle.highlight)
            if (start >= 0) {
                addStyle(SpanStyle(color = highlightColor), start, start + subtitle.highlight.length)
            }
        }
    }
    Text(
        text = text,
        color = TangemTheme.colors2.text.neutral.tertiary,
        style = TangemTheme.typography2.captionMedium12,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun ContentSubtitle.Direction.templateResId(): Int = when (this) {
    ContentSubtitle.Direction.TO -> R.string.transaction_history_to_inline_address
    ContentSubtitle.Direction.FROM -> R.string.transaction_history_from_inline_address
}

// endregion

// region Amount

@Composable
private fun AmountText(amount: String, status: Status, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    val display = if (status is Status.Failed) amount.stripLeadingSign() else amount
    Text(
        text = display.orMaskWithStars(isBalanceHidden),
        color = if (status is Status.Confirmed) {
            TangemTheme.colors2.text.neutral.primary
        } else {
            TangemTheme.colors2.text.neutral.tertiary
        },
        textDecoration = if (status is Status.Failed) TextDecoration.LineThrough else null,
        style = TangemTheme.typography2.bodyMedium16,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun CurrencyText(symbol: String, modifier: Modifier = Modifier) {
    Text(
        text = symbol,
        color = TangemTheme.colors2.text.neutral.tertiary,
        style = TangemTheme.typography2.captionMedium12,
        maxLines = 1,
        modifier = modifier,
    )
}

private fun String.stripLeadingSign(): String = when {
    startsWith('+') || startsWith('-') || startsWith('−') -> drop(1).trim()
    else -> this
}

// endregion

// region Preview

@Suppress("LongParameterList")
private fun previewContent(
    txHash: String,
    iconRes: Int,
    direction: Direction,
    status: Status,
    title: String,
    subtitle: String,
    amount: String,
    currencySymbol: String = "USDT",
): TransactionItemUM.Content = TransactionItemUM.Content(
    txHash = txHash,
    amount = amount,
    currencySymbol = currencySymbol,
    time = "",
    status = status,
    direction = direction,
    onClick = {},
    iconRes = iconRes,
    title = stringReference(title),
    subtitle = ContentSubtitle.Plain(stringReference(subtitle)),
    timestamp = 0L,
)

@Composable
private fun PreviewColumn(items: List<TransactionItemUM>) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors2.surface.level1)
            .padding(TangemTheme.dimens2.x2),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        items.forEach { TransactionItem(state = it, isBalanceHidden = false) }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionItem_Receive() {
    TangemThemePreviewRedesign {
        PreviewColumn(
            items = listOf(
                previewContent(
                    txHash = "rcv-c",
                    iconRes = R.drawable.ic_arrow_down_24,
                    direction = Direction.INCOMING,
                    status = Status.Confirmed,
                    title = "Received",
                    subtitle = "from: 33BdfS...ga2B",
                    amount = "+350.00",
                ),
                previewContent(
                    txHash = "rcv-u",
                    iconRes = R.drawable.ic_arrow_down_24,
                    direction = Direction.INCOMING,
                    status = Status.Unconfirmed,
                    title = "Receiving",
                    subtitle = "from: 33BdfS...ga2B",
                    amount = "+350.00",
                ),
                previewContent(
                    txHash = "rcv-f",
                    iconRes = R.drawable.ic_close_24,
                    direction = Direction.INCOMING,
                    status = Status.Failed,
                    title = "Receiving failed",
                    subtitle = "from: 33BdfS...ga2B",
                    amount = "350.00",
                ),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionItem_Send() {
    TangemThemePreviewRedesign {
        PreviewColumn(
            items = listOf(
                previewContent(
                    txHash = "snd-c",
                    iconRes = R.drawable.ic_arrow_up_24,
                    direction = Direction.OUTGOING,
                    status = Status.Confirmed,
                    title = "Sent",
                    subtitle = "to: 33BdfS...ga2B",
                    amount = "-350.31",
                ),
                previewContent(
                    txHash = "snd-u",
                    iconRes = R.drawable.ic_arrow_up_24,
                    direction = Direction.OUTGOING,
                    status = Status.Unconfirmed,
                    title = "Sending",
                    subtitle = "to: 33BdfS...ga2B",
                    amount = "+350.31",
                ),
                previewContent(
                    txHash = "snd-f",
                    iconRes = R.drawable.ic_close_24,
                    direction = Direction.OUTGOING,
                    status = Status.Failed,
                    title = "Sending failed",
                    subtitle = "to: 33BdfS...ga2B",
                    amount = "350.31",
                ),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionItem_Swap() {
    TangemThemePreviewRedesign {
        PreviewColumn(
            items = listOf(
                previewContent(
                    txHash = "swp-c",
                    iconRes = R.drawable.ic_exchange_vertical_24,
                    direction = Direction.INCOMING,
                    status = Status.Confirmed,
                    title = "Swapped",
                    subtitle = "to: POL",
                    amount = "+350.00",
                ),
                previewContent(
                    txHash = "swp-u",
                    iconRes = R.drawable.ic_exchange_vertical_24,
                    direction = Direction.INCOMING,
                    status = Status.Unconfirmed,
                    title = "Swapping",
                    subtitle = "to: POL",
                    amount = "+350.00",
                ),
                previewContent(
                    txHash = "swp-f",
                    iconRes = R.drawable.ic_close_24,
                    direction = Direction.INCOMING,
                    status = Status.Failed,
                    title = "Swapping failed",
                    subtitle = "to: POL",
                    amount = "350.00",
                ),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionItem_Express() {
    TangemThemePreviewRedesign {
        PreviewColumn(
            items = listOf(
                TransactionItemUM.Content(
                    txHash = "exp-swap-u",
                    amount = "-390.00",
                    currencySymbol = "USDT",
                    time = "",
                    status = Status.Unconfirmed,
                    direction = Direction.OUTGOING,
                    onClick = {},
                    iconRes = R.drawable.ic_exchange_vertical_24,
                    title = stringReference("Swapping"),
                    subtitle = ContentSubtitle.Asset(
                        direction = ContentSubtitle.Direction.TO,
                        symbol = "POL",
                        icon = CurrencyIconState.CoinIcon(
                            url = null,
                            fallbackResId = R.drawable.ic_custom_token_44,
                            isGrayscale = false,
                            shouldShowCustomBadge = false,
                        ),
                    ),
                    timestamp = 0L,
                    warning = stringReference("KYC verification required by provider"),
                ),
                TransactionItemUM.Content(
                    txHash = "exp-onramp-c",
                    amount = "+0.006339",
                    currencySymbol = "BTC",
                    time = "",
                    status = Status.Confirmed,
                    direction = Direction.INCOMING,
                    onClick = {},
                    iconRes = R.drawable.ic_tangem_card_24,
                    title = stringReference("Topped up"),
                    subtitle = ContentSubtitle.Asset(
                        direction = ContentSubtitle.Direction.FROM,
                        symbol = "SEK",
                        icon = null,
                    ),
                    timestamp = 0L,
                ),
                TransactionItemUM.Content(
                    txHash = "exp-onramp-f",
                    amount = "0.006339",
                    currencySymbol = "BTC",
                    time = "",
                    status = Status.Failed,
                    direction = Direction.INCOMING,
                    onClick = {},
                    iconRes = R.drawable.ic_tangem_card_24,
                    title = stringReference("Top up failed"),
                    subtitle = ContentSubtitle.Asset(
                        direction = ContentSubtitle.Direction.FROM,
                        symbol = "SEK",
                        icon = null,
                    ),
                    timestamp = 0L,
                ),
            ),
        )
    }
}

// endregion