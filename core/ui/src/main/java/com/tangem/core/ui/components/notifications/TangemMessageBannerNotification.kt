package com.tangem.core.ui.components.notifications

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.notifications.NotificationConfig.ButtonsState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.CloseButton
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.TangemTheme

/**
 * Driver that renders a DS3 [TangemMessageBanner] from a legacy [NotificationConfig], so existing
 * notification call sites can adopt the new banner without rebuilding their models.
 *
 * @param config Legacy notification model.
 * @param variant Banner appearance. See [TangemMessageBanner.Variant].
 * @param contentAlign Text alignment. See [TangemMessageBanner.ContentAlign].
 */
@Deprecated("Use as migration solution, not production one")
@Composable
fun TangemMessageBanner(
    config: NotificationConfig,
    modifier: Modifier = Modifier,
    variant: TangemMessageBanner.Variant = TangemMessageBanner.Variant.Default,
    contentAlign: TangemMessageBanner.ContentAlign = TangemMessageBanner.ContentAlign.Start,
) {
    val onClick = config.onClick
    val (secondaryButton, primaryButton) = config.buttonsState.toBannerButtons()
    val icon = config.toBannerIcon()

    TangemMessageBanner(
        modifier = if (onClick != null) modifier.clickableSingle(onClick = onClick) else modifier,
        variant = variant,
        contentAlign = contentAlign,
        title = config.title ?: config.subtitle,
        description = config.title?.let { config.subtitle },
        secondaryButton = secondaryButton,
        primaryButton = primaryButton,
        slotStart = icon?.let { iconUM ->
            { TangemIcon(tangemIconUM = iconUM, modifier = Modifier.size(config.iconSize)) }
        },
        slotEnd = config.onCloseClick?.let { onClose ->
            { TangemMessageBanner.CloseButton(onClick = onClose) }
        },
    )
}

/**
 * Builds the leading icon, honouring the remote-url, untinted, and tinted cases of [NotificationConfig].
 * Returns `null` when the config carries no icon (no url and an unset [NotificationConfig.iconResId]),
 * so the banner hides the leading slot instead of rendering a broken one.
 */
private fun NotificationConfig.toBannerIcon(): TangemIconUM? = when {
    iconUrl != null -> TangemIconUM.Url(url = iconUrl, fallbackRes = iconResId)
    iconResId == 0 -> null
    iconTint == NotificationConfig.IconTint.Unspecified -> TangemIconUM.Image(imageRes = iconResId)
    else -> TangemIconUM.Icon(iconRes = iconResId, tintReference = ColorReference2 { iconTint.toColor() })
}

@Composable
private fun NotificationConfig.IconTint.toColor() = when (this) {
    NotificationConfig.IconTint.Unspecified -> TangemTheme.colors3.icon.primary
    NotificationConfig.IconTint.Accent -> TangemTheme.colors3.icon.status.info
    NotificationConfig.IconTint.Attention -> TangemTheme.colors3.icon.status.warning
    NotificationConfig.IconTint.Warning -> TangemTheme.colors3.icon.status.warning
}

/** Maps the legacy button configuration onto the banner's (secondary = start, primary = end) pair. */
private fun ButtonsState?.toBannerButtons(): Pair<TangemMessageBanner.Button?, TangemMessageBanner.Button?> =
    when (this) {
        is ButtonsState.PrimaryButtonConfig -> null to TangemMessageBanner.Button(
            text = text,
            onClick = onClick,
            iconEnd = iconResId?.let { TangemIconUM.Icon(iconRes = it) },
            isLoading = shouldShowProgress,
        )
        is ButtonsState.SecondaryButtonConfig -> TangemMessageBanner.Button(
            text = text,
            onClick = onClick,
            iconEnd = iconResId?.let { TangemIconUM.Icon(iconRes = it) },
            isLoading = shouldShowProgress,
        ) to null
        is ButtonsState.PairButtonsConfig -> TangemMessageBanner.Button(
            text = secondaryText,
            onClick = onSecondaryClick,
        ) to TangemMessageBanner.Button(text = primaryText, onClick = onPrimaryClick)
        is ButtonsState.SecondaryPairButtonsConfig -> TangemMessageBanner.Button(
            text = leftText,
            onClick = onLeftClick,
        ) to TangemMessageBanner.Button(text = rightText, onClick = onRightClick)
        null -> null to null
    }