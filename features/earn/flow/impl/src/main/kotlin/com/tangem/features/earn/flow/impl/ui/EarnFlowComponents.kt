@file:Suppress("MagicNumber")

package com.tangem.features.earn.flow.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.components.buttons.predefined.PredefinedPercentButtonUM
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.notifications.toBannerButtons
import com.tangem.core.ui.ds.button.TangemButtonIconPosition
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_warning_20
import com.tangem.features.earn.api.ui.EarnRowUM
import com.tangem.features.earn.flow.impl.ui.state.EarnIconsUM
import kotlinx.collections.immutable.ImmutableList

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
internal fun EarnFlowScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    bottomExtraTop: (@Composable () -> Unit)? = null,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .imePadding(),
    ) {
        topBar()
        content()
        TangemFooter(extraTop = bottomExtraTop, fadeEnabled = false, content = bottom)
    }
}

@Composable
internal fun EarnLogos(icons: EarnIconsUM, size: Dp, overlap: Dp, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(-overlap)) {
        CurrencyIcon(
            state = icons.currency,
            shouldDisplayNetwork = false,
            iconSize = size,
            modifier = Modifier.zIndex(1f),
        )
        AsyncImage(
            model = icons.provider.getReference(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
        )
    }
}

@Composable
internal fun EarnNotice(text: TextReference, modifier: Modifier = Modifier) {
    TangemMessageBanner(
        title = text,
        modifier = modifier.fillMaxWidth(),
        variant = TangemMessageBanner.Variant.Info,
        slotEnd = {
            Icon(
                imageVector = Icons.ic_clock_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
            )
        },
    )
}

/**
 * Rows of a product, label on the start and value on the end. A row with an explanation opens it on click.
 *
 * [EarnRowsStyle.Card] is a list on a card: secondary labels, primary values. [EarnRowsStyle.Details] is the
 * compact block of a result: primary labels, secondary values.
 */
@Composable
internal fun EarnRows(
    rows: ImmutableList<EarnRowUM>,
    modifier: Modifier = Modifier,
    style: EarnRowsStyle = EarnRowsStyle.Card,
) {
    val isCard = style == EarnRowsStyle.Card
    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            TangemRow(
                modifier = if (isCard) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                divider = isCard && index < rows.lastIndex,
                includeInnerPaddings = isCard,
                titleSlot = {
                    RowText(
                        text = row.label,
                        isPrimary = !isCard,
                    )
                },
                valueSlot = {
                    RowText(
                        text = row.value,
                        isPrimary = isCard,
                        textAlign = TextAlign.End,
                    )
                },
                onClick = row.onInfoClick,
            )
            if (!isCard && index < rows.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = TangemTheme.colors3.border.secondary,
                )
            }
        }
    }
}

internal enum class EarnRowsStyle { Card, Details }

@Composable
private fun RowText(text: TextReference, isPrimary: Boolean, textAlign: TextAlign? = null) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography3.body.medium,
        color = if (isPrimary) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
        textAlign = textAlign,
        maxLines = 1,
    )
}

/** The fee and balance cards shared with Send, drawn as message banners: the type picks the colour and the icon. */
@Composable
internal fun EarnNotifications(notifications: ImmutableList<NotificationUM>) {
    notifications.forEach { notification -> EarnNotificationBanner(notification) }
}

@Composable
private fun EarnNotificationBanner(notification: NotificationUM) {
    val config = notification.config
    val (secondary, primary) = config.buttonsState.toBannerButtons()
    val isError = notification is NotificationUM.Error
    val isInfo = notification is NotificationUM.Info
    TangemMessageBanner(
        title = config.title ?: config.subtitle,
        description = config.subtitle.takeIf { config.title != null },
        modifier = Modifier.fillMaxWidth(),
        variant = when {
            isError -> TangemMessageBanner.Variant.Error
            isInfo -> TangemMessageBanner.Variant.Info
            else -> TangemMessageBanner.Variant.Warning
        },
        onClick = config.onClick,
        secondaryButton = secondary,
        primaryButton = primary,
        slotEnd = {
            Icon(
                imageVector = if (isInfo) Icons.ic_info_20 else Icons.ic_warning_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
            )
        },
    )
}

@Composable
internal fun EarnSecondaryText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun EarnPercentButtons(items: ImmutableList<PredefinedPercentButtonUM>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            TangemButton(
                modifier = Modifier.weight(1f),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X8,
                text = item.label,
                onClick = item.onClick,
            )
        }
    }
}

/** Only `text`, `type`, `tangemIconUM`, `iconPosition`, the states and `onClick` of [TangemButtonUM] apply. */
@Composable
internal fun EarnFlowButton(button: TangemButtonUM, modifier: Modifier = Modifier) {
    TangemButton(
        modifier = modifier,
        variant = if (button.type == TangemButtonType.Primary) {
            TangemButton.Variant.Primary
        } else {
            TangemButton.Variant.Secondary
        },
        size = TangemButton.Size.X12,
        text = button.text,
        isEnabled = button.isEnabled,
        isDimmed = button.isDimmed,
        isLoading = button.isLoading,
        iconStart = button.tangemIconUM.takeIf { button.iconPosition == TangemButtonIconPosition.Start },
        iconEnd = button.tangemIconUM.takeIf { button.iconPosition == TangemButtonIconPosition.End },
        onClick = button.onClick,
    )
}