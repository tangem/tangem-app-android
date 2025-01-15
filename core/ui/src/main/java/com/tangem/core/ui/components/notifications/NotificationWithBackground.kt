package com.tangem.core.ui.components.notifications

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemColorPalette.Dark6
import com.tangem.core.ui.res.TangemColorPalette.Light4
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Custom notification with image background
 * @see [Swap Promo](https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=8713-6307&mode=dev)
 */
@Suppress("LongMethod", "DestructuringDeclarationWithTooManyEntries")
@Composable
fun NotificationWithBackground(config: NotificationConfig, modifier: Modifier = Modifier) {
    val button = config.buttonsState as? NotificationConfig.ButtonsState.SecondaryButtonConfig

    ConstraintLayout(
        modifier = modifier
            .defaultMinSize(minHeight = TangemTheme.dimens.size62)
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .clickable(
                enabled = config.onClick != null,
                onClick = config.onClick ?: {},
            ),
    ) {
        val (iconRef, titleRef, subtitleRef, closeIconRef, buttonRef, backgroundRef) = createRefs()

        val spacing2 = TangemTheme.dimens.spacing2
        val spacing12 = TangemTheme.dimens.spacing12
        val spacing14 = TangemTheme.dimens.spacing14

        Image(
            painter = painterResource(config.backgroundResId ?: R.drawable.img_swap_promo_blue_banner_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.constrainAs(backgroundRef) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                visibility = if (config.backgroundResId == null) Visibility.Gone else Visibility.Visible
            },
        )
        Image(
            painter = painterResource(config.iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(TangemTheme.dimens.size34)
                .constrainAs(iconRef) {
                    start.linkTo(parent.start, spacing12)
                    linkTo(titleRef.top, subtitleRef.bottom, bias = 0.1f)
                },
        )

        val titleText = config.title?.resolveReference()
        if (titleText != null) {
            Text(
                text = titleText,
                style = TangemTheme.typography.button,
                color = TangemTheme.colors.text.constantWhite,
                modifier = Modifier.constrainAs(titleRef) {
                    top.linkTo(parent.top, spacing12)
                    start.linkTo(iconRef.end, spacing12)
                    end.linkTo(closeIconRef.start, spacing2)
                    width = Dimension.fillToConstraints
                },
            )
        }

        Text(
            text = config.subtitle.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.constantWhite,
            modifier = Modifier.constrainAs(subtitleRef) {
                top.linkTo(titleRef.bottom, spacing2)
                start.linkTo(iconRef.end, spacing12)
                end.linkTo(parent.end, spacing12)
                bottom.linkTo(buttonRef.top, spacing12, spacing14)
                width = Dimension.fillToConstraints
            },
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_close_24),
            contentDescription = null,
            tint = TangemTheme.colors.text.constantWhite,
            modifier = Modifier
                .size(TangemTheme.dimens.size16)
                .constrainAs(closeIconRef) {
                    top.linkTo(parent.top, spacing12)
                    end.linkTo(parent.end, spacing12)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                ) {
                    config.onCloseClick?.invoke()
                },
        )
        val isDarkMode = LocalIsInDarkTheme.current
        TangemButton(
            modifier = Modifier.constrainAs(buttonRef) {
                start.linkTo(parent.start, spacing12)
                end.linkTo(parent.end, spacing12)
                bottom.linkTo(parent.bottom, spacing12)
                width = Dimension.fillToConstraints
                visibility = if (button == null) {
                    Visibility.Gone
                } else {
                    Visibility.Visible
                }
            },
            text = button?.text?.resolveReference().orEmpty(),
            icon = TangemButtonIconPosition.Start(button?.iconResId ?: R.drawable.ic_exchange_vertical_24),
            onClick = button?.onClick ?: {},
            colors = ButtonColors(
                containerColor = if (isDarkMode) Light4 else TangemTheme.colors.button.secondary,
                contentColor = Dark6,
                disabledContainerColor = TangemTheme.colors.button.disabled,
                disabledContentColor = TangemTheme.colors.text.disabled,
            ),
            textStyle = TangemTheme.typography.subtitle1,
            enabled = true,
            showProgress = false,
        )
    }
}

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NotificationWithBackgroundPreview(
    @PreviewParameter(NotificationWithBackgroundPreviewProvider::class) config: NotificationConfig,
) {
    TangemThemePreview {
        NotificationWithBackground(config = config)
    }
}

private class NotificationWithBackgroundPreviewProvider : PreviewParameterProvider<NotificationConfig> {
    override val values: Sequence<NotificationConfig>
        get() = sequenceOf(
            NotificationConfig(
                title = resourceReference(id = R.string.main_swap_changelly_promotion_title),
                subtitle = resourceReference(
                    id = R.string.main_swap_changelly_promotion_message,
                    formatArgs = wrappedList("1", "2", "3"),
                ),
                iconResId = R.drawable.img_swap_promo,
                backgroundResId = R.drawable.img_swap_promo_blue_banner_background,
            ),
            NotificationConfig(
                title = resourceReference(
                    id = R.string.token_swap_changelly_promotion_title,
                    formatArgs = wrappedList("1"),
                ),
                subtitle = stringReference(
                    "Swap multiple currencies between any chains you wish. Swap multiple " +
                        "currencies between any chains you wish. Swap multiple " +
                        "currencies between any chains you wish. Commission free period " +
                        "till Dec 31.",
                ),
                iconResId = R.drawable.img_swap_promo,
                backgroundResId = R.drawable.img_swap_promo_blue_banner_background,
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(id = R.string.token_swap_promotion_button),
                    onClick = {},
                ),
            ),
        )
}
//endregion