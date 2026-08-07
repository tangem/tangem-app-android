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
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.PaymentAccountIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Direction
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.components.transactions.state.asImageVector
import com.tangem.core.ui.ds.image.DeviceIconUM
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
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.core.ui.res.generated.icons.ic_cross_20
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
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
        ) {
            StatusCircle(
                icon = state.icon,
                status = state.status,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.HEAD)
                    .padding(end = 12.dp)
                    .size(40.dp)
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
                    .padding(top = 2.dp),
            )
            state.amount?.let { amount ->
                AmountText(
                    amount = amount,
                    status = state.status,
                    isBalanceHidden = isBalanceHidden,
                    modifier = Modifier
                        .layoutId(TangemRowLayoutId.END_TOP)
                        .testTag(TransactionHistoryItemTestTags.AMOUNT),
                )
            }
            CurrencyText(
                symbol = state.currencySymbol,
                modifier = Modifier
                    .layoutId(TangemRowLayoutId.END_BOTTOM)
                    .padding(top = 2.dp)
                    .testTag(TransactionHistoryItemTestTags.CURRENCY),
            )
        }
        state.warning?.let { warning ->
            WarningLine(
                warning = warning,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 12.dp,
                ),
            )
        }
    }
}

@Composable
private fun WarningLine(warning: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alert_triangle_20),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.warning,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = warning.resolveReference(),
            color = TangemTheme.colors3.text.status.warning,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// region Status circle

@Composable
private fun StatusCircle(icon: TxIcon, status: Status, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = status.backgroundColor,
            shape = CircleShape,
        ),
    ) {
        Icon(
            imageVector = icon.asImageVector(),
            contentDescription = null,
            tint = status.iconTint,
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.Center),
        )
    }
}

private val Status.backgroundColor: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors3.bg.opaque.secondary
        is Status.Unconfirmed -> TangemTheme.colors3.bg.status.infoSubtle
        is Status.Failed -> TangemTheme.colors3.bg.status.errorSubtle
    }

private val Status.iconTint: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors3.icon.primary
        is Status.Unconfirmed -> TangemTheme.colors3.icon.brand
        is Status.Failed -> TangemTheme.colors3.icon.status.error
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
        style = TangemTheme.typography3.body.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private val Status.titleColor: Color
    @Composable get() = when (this) {
        is Status.Confirmed -> TangemTheme.colors3.text.primary
        is Status.Unconfirmed -> TangemTheme.colors3.text.brand
        is Status.Failed -> TangemTheme.colors3.text.status.error
    }

@Suppress("LongMethod")
@Composable
private fun SubtitleText(subtitle: ContentSubtitle, status: Status, modifier: Modifier = Modifier) {
    val textStyle = TangemTheme.typography3.caption.medium
    val secondary = TangemTheme.colors3.text.secondary
    val primary = TangemTheme.colors3.text.primary
    val isFailed = status is Status.Failed
    when (subtitle) {
        is ContentSubtitle.Plain -> Text(
            text = subtitle.text.resolveReference(),
            color = secondary,
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
            color = secondary,
            afterIconColor = if (isFailed) secondary else primary,
            textStyle = TangemTheme.typography3.caption.medium,
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
            color = secondary,
            afterIconColor = if (isFailed) secondary else primary,
            textStyle = TangemTheme.typography3.caption.medium,
            modifier = modifier,
        ) {
            val backgroundColor = if (isFailed) {
                TangemTheme.colors3.bg.disabled
            } else {
                subtitle.iconBackgroundColor
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(backgroundColor),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = subtitle.iconResId),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.staticDark,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
        is ContentSubtitle.OwnPaymentAccount -> InlineImageSubtitle(
            template = stringResourceSafe(
                subtitle.direction.templateResId(),
                subtitle.accountName.resolveReference(),
            ),
            color = secondary,
            afterIconColor = primary,
            textStyle = TangemTheme.typography3.caption.medium,
            modifier = modifier,
        ) {
            PaymentAccountIcon(size = AccountIconSize.ExtraSmall)
        }
        is ContentSubtitle.OwnWallet -> OwnWalletSubtitle(subtitle = subtitle, modifier = modifier)
        is ContentSubtitle.Asset -> AssetSubtitle(subtitle = subtitle, status = status, modifier = modifier)
    }
}

/**
 * Express counterparty subtitle: "to:/from: {tokenIcon} {SYMBOL}" and, when the leg settled in a different own
 * portfolio, the "in {owner}" tail. The whole phrasing (direction prefix, the "… in …" connector and word order) lives
 * in string resources: the owner tail (`token_details_toolbar_title_token_in_account` / `…_in_wallet`) is nested into
 * the direction template as its argument, yielding one localized string with two `%image%` markers. Both icons are
 * inline placeholders in a single ellipsizable [Text]; account / payment put the icon before the name, wallet the name
 * before the device icon — as the resources already encode.
 */
@Composable
private fun AssetSubtitle(subtitle: ContentSubtitle.Asset, status: Status, modifier: Modifier = Modifier) {
    val secondary = TangemTheme.colors3.text.secondary
    val primary = TangemTheme.colors3.text.primary
    // Only the values (symbol, owner name) are highlighted; the "to:"/"in" template literals stay secondary.
    val valueColor = if (status is Status.Failed) secondary else primary

    val owner = subtitle.owner
    val ownerName = when (owner) {
        is ContentSubtitle.AssetOwner.Account -> owner.name.resolveReference()
        is ContentSubtitle.AssetOwner.PaymentAccount -> owner.name.resolveReference()
        is ContentSubtitle.AssetOwner.Wallet -> owner.name
        null -> null
    }
    // The owner tail is nested into the direction template as its %1$s, so the whole "to:/from: … in …" phrasing and
    // its word order live in string resources; the wallet template puts its icon after the name, the account one before.
    val template = when (owner) {
        null -> stringResourceSafe(subtitle.direction.templateResId(), subtitle.symbol)
        is ContentSubtitle.AssetOwner.Wallet -> stringResourceSafe(
            subtitle.direction.templateResId(),
            stringResourceSafe(R.string.token_details_toolbar_title_token_in_wallet, subtitle.symbol, owner.name),
        )
        else -> stringResourceSafe(
            subtitle.direction.templateResId(),
            stringResourceSafe(
                R.string.token_details_toolbar_title_token_in_account,
                subtitle.symbol,
                ownerName.orEmpty(),
            ),
        )
    }

    // Icons in the order their %image% markers appear in the composed template: token first, then the owner (if any).
    val icons = buildList<@Composable () -> Unit> {
        add {
            subtitle.icon?.let { iconState ->
                CurrencyIcon(
                    state = iconState,
                    shouldDisplayNetwork = false,
                    withFixedSize = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        if (owner != null) add { AssetOwnerIcon(owner = owner, isFailed = status is Status.Failed) }
    }

    InlineImagesText(
        template = template,
        icons = icons,
        color = secondary,
        highlights = listOfNotNull(subtitle.symbol, ownerName),
        highlightColor = valueColor,
        textStyle = TangemTheme.typography3.caption.medium,
        modifier = modifier,
    )
}

@Composable
private fun AssetOwnerIcon(owner: ContentSubtitle.AssetOwner?, isFailed: Boolean) {
    when (owner) {
        is ContentSubtitle.AssetOwner.Account -> {
            val backgroundColor = if (isFailed) {
                TangemTheme.colors3.bg.disabled
            } else {
                owner.iconBackgroundColor
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(backgroundColor),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = owner.iconResId),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.staticDark,
                    modifier = Modifier.size(10.dp),
                )
            }
        }
        is ContentSubtitle.AssetOwner.PaymentAccount -> PaymentAccountIcon(size = AccountIconSize.ExtraSmall)
        is ContentSubtitle.AssetOwner.Wallet -> TangemDeviceIcon(
            state = owner.deviceIconUM,
            modifier = Modifier.fillMaxSize(),
        )
        null -> Unit
    }
}

@Composable
private fun PlainAddressText(subtitle: ContentSubtitle.PlainAddress, status: Status, modifier: Modifier = Modifier) {
    val full = subtitle.text.resolveReference()
    val highlightColor = if (status is Status.Failed) {
        TangemTheme.colors3.text.secondary
    } else {
        TangemTheme.colors3.text.primary
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
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Own-wallet counterparty subtitle: "to:/from: {wallet name}" followed by the wallet's device icon. Unlike the
 * address/account/asset rows (icon before the value via [InlineImageSubtitle]), the wallet renders name-first with a
 * trailing icon, so a plain [Row] is used — the name ellipsizes within the available width while the icon stays visible
 * (an inline trailing icon would be truncated by the ellipsis instead).
 */
@Composable
private fun OwnWalletSubtitle(subtitle: ContentSubtitle.OwnWallet, modifier: Modifier = Modifier) {
    val primary = TangemTheme.colors3.text.primary
    val full = stringResourceSafe(subtitle.direction.plainTemplateResId(), subtitle.walletName)
    val text = remember(full, subtitle.walletName, primary) {
        buildAnnotatedString {
            append(full)
            val start = full.lastIndexOf(subtitle.walletName)
            if (start >= 0) {
                addStyle(SpanStyle(color = primary), start, start + subtitle.walletName.length)
            }
        }
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = text,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
        TangemDeviceIcon(
            state = subtitle.deviceIconUM,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun ContentSubtitle.Direction.templateResId(): Int = when (this) {
    ContentSubtitle.Direction.TO -> R.string.transaction_history_to_inline_address
    ContentSubtitle.Direction.FROM -> R.string.transaction_history_from_inline_address
}

private fun ContentSubtitle.Direction.plainTemplateResId(): Int = when (this) {
    ContentSubtitle.Direction.TO -> R.string.transaction_history_transaction_to_address
    ContentSubtitle.Direction.FROM -> R.string.transaction_history_transaction_from_address
}

// endregion

// region Amount

@Composable
private fun AmountText(amount: String, status: Status, isBalanceHidden: Boolean, modifier: Modifier = Modifier) {
    val display = if (status is Status.Failed) amount.stripLeadingSign() else amount
    Text(
        text = display.orMaskWithStars(isBalanceHidden),
        color = if (status is Status.Confirmed) {
            TangemTheme.colors3.text.primary
        } else {
            TangemTheme.colors3.text.secondary
        },
        textDecoration = if (status is Status.Failed) TextDecoration.LineThrough else null,
        style = TangemTheme.typography3.body.medium,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun CurrencyText(symbol: String, modifier: Modifier = Modifier) {
    Text(
        text = symbol,
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
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
    icon: TxIcon,
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
    icon = icon,
    title = stringReference(title),
    subtitle = ContentSubtitle.Plain(stringReference(subtitle)),
    timestamp = 0L,
)

@Composable
private fun PreviewColumn(items: List<TransactionItemUM>) {
    Column(
        modifier = Modifier
            .background(TangemTheme.colors3.bg.primary)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    icon = TxIcon.Vector(Icons.ic_arrow_down_20),
                    direction = Direction.INCOMING,
                    status = Status.Confirmed,
                    title = "Received",
                    subtitle = "from: 33BdfS...ga2B",
                    amount = "+350.00",
                ),
                previewContent(
                    txHash = "rcv-u",
                    icon = TxIcon.Vector(Icons.ic_arrow_down_20),
                    direction = Direction.INCOMING,
                    status = Status.Unconfirmed,
                    title = "Receiving",
                    subtitle = "from: 33BdfS...ga2B",
                    amount = "+350.00",
                ),
                previewContent(
                    txHash = "rcv-f",
                    icon = TxIcon.Vector(Icons.ic_cross_20),
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
                    icon = TxIcon.Vector(Icons.ic_arrow_up_20),
                    direction = Direction.OUTGOING,
                    status = Status.Confirmed,
                    title = "Sent",
                    subtitle = "to: 33BdfS...ga2B",
                    amount = "-350.31",
                ),
                previewContent(
                    txHash = "snd-u",
                    icon = TxIcon.Vector(Icons.ic_arrow_up_20),
                    direction = Direction.OUTGOING,
                    status = Status.Unconfirmed,
                    title = "Sending",
                    subtitle = "to: 33BdfS...ga2B",
                    amount = "+350.31",
                ),
                previewContent(
                    txHash = "snd-f",
                    icon = TxIcon.Vector(Icons.ic_cross_20),
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
                    icon = TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20),
                    direction = Direction.INCOMING,
                    status = Status.Confirmed,
                    title = "Swapped",
                    subtitle = "to: POL",
                    amount = "+350.00",
                ),
                previewContent(
                    txHash = "swp-u",
                    icon = TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20),
                    direction = Direction.INCOMING,
                    status = Status.Unconfirmed,
                    title = "Swapping",
                    subtitle = "to: POL",
                    amount = "+350.00",
                ),
                previewContent(
                    txHash = "swp-f",
                    icon = TxIcon.Vector(Icons.ic_cross_20),
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
                    icon = TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20),
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
                    icon = TxIcon.Vector(Icons.ic_card_20),
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
                    icon = TxIcon.Vector(Icons.ic_card_20),
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

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TransactionItem_Express_Owner() {
    val tokenIcon = CurrencyIconState.CoinIcon(
        url = null,
        fallbackResId = R.drawable.ic_custom_token_44,
        isGrayscale = false,
        shouldShowCustomBadge = false,
    )
    TangemThemePreviewRedesign {
        PreviewColumn(
            items = listOf(
                // Cross-account swap: "to: {token} POL in {accountIcon} Family".
                TransactionItemUM.Content(
                    txHash = "exp-swap-account",
                    amount = "-390.00",
                    currencySymbol = "USDT",
                    time = "",
                    status = Status.Confirmed,
                    direction = Direction.OUTGOING,
                    onClick = {},
                    icon = TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20),
                    title = stringReference("Swapped"),
                    subtitle = ContentSubtitle.Asset(
                        direction = ContentSubtitle.Direction.TO,
                        symbol = "POL",
                        icon = tokenIcon,
                        owner = ContentSubtitle.AssetOwner.Account(
                            name = stringReference("Family"),
                            iconResId = R.drawable.ic_wallet_24,
                            iconBackgroundColor = Color(0xFF0099FF),
                        ),
                    ),
                    timestamp = 0L,
                ),
                // Cross-wallet swap: "from: {token} ETH in My Wallet {deviceIcon}".
                TransactionItemUM.Content(
                    txHash = "exp-swap-wallet",
                    amount = "+0.006339",
                    currencySymbol = "BTC",
                    time = "",
                    status = Status.Confirmed,
                    direction = Direction.INCOMING,
                    onClick = {},
                    icon = TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20),
                    title = stringReference("Swapped"),
                    subtitle = ContentSubtitle.Asset(
                        direction = ContentSubtitle.Direction.FROM,
                        symbol = "ETH",
                        icon = tokenIcon,
                        owner = ContentSubtitle.AssetOwner.Wallet(
                            name = "My Wallet",
                            deviceIconUM = DeviceIconUM.Card(mainColor = Color(0xFF1E1E1E), secondColor = null),
                        ),
                    ),
                    timestamp = 0L,
                ),
            ),
        )
    }
}

// endregion