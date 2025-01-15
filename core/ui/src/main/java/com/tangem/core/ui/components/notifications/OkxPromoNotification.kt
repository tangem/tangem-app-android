package com.tangem.core.ui.components.notifications

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemColorPalette.Dark6
import com.tangem.core.ui.res.TangemColorPalette.Light4
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

private val OxkPromoColor = Color(0xFFBCFF2F)

@Composable
fun OkxPromoNotification(config: NotificationConfig, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(Dark6),
    ) {
        Content(config = config)
        Button(config = config)
    }
}

@Composable
private fun Content(config: NotificationConfig) {
    Row {
        Icon(
            painter = rememberVectorPainter(image = ImageVector.vectorResource(id = R.drawable.img_okx_dex_logo)),
            contentDescription = null,
            tint = TangemTheme.colors.icon.constant,
            modifier = Modifier
                .padding(start = TangemTheme.dimens.spacing12)
                .align(Alignment.CenterVertically),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing2),
            modifier = Modifier
                .weight(1f)
                .padding(TangemTheme.dimens.spacing12),
        ) {
            val titleText = config.title?.resolveReference()

            if (titleText != null) {
                Text(
                    text = titleText,
                    style = TangemTheme.typography.button,
                    color = OxkPromoColor,
                )
            }

            Text(
                text = config.subtitle.resolveReference(),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.constantWhite,
            )
        }
        config.onCloseClick?.let {
            Icon(
                painter = rememberVectorPainter(
                    image = ImageVector.vectorResource(R.drawable.ic_close_24),
                ),
                contentDescription = null,
                tint = TangemTheme.colors.icon.constant,
                modifier =
                Modifier
                    .padding(
                        top = TangemTheme.dimens.spacing8,
                        end = TangemTheme.dimens.spacing8,
                    )
                    .size(TangemTheme.dimens.size20)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(radius = TangemTheme.dimens.radius10),
                        onClick = it,
                    ),
            )
        }
    }
}

@Composable
private fun Button(config: NotificationConfig) {
    val button = config.buttonsState as? NotificationConfig.ButtonsState.SecondaryButtonConfig

    button?.let {
        val isDarkMode = LocalIsInDarkTheme.current
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = TangemTheme.dimens.spacing12,
                    end = TangemTheme.dimens.spacing12,
                    bottom = TangemTheme.dimens.spacing12,
                ),
            text = button.text.resolveReference(),
            icon = TangemButtonIconPosition.Start(button.iconResId ?: R.drawable.ic_exchange_vertical_24),
            onClick = button.onClick,
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

// region Preview
@Preview(widthDp = 360)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OkxPromoNotification_Preview(
    @PreviewParameter(OkxPromoNotificationPreviewProvider::class) data: NotificationConfig,
) {
    TangemThemePreview {
        OkxPromoNotification(data)
    }
}

private class OkxPromoNotificationPreviewProvider : PreviewParameterProvider<NotificationConfig> {
    override val values: Sequence<NotificationConfig>
        get() = sequenceOf(
            NotificationConfig(
                title = resourceReference(R.string.swap_promo_title),
                subtitle = resourceReference(R.string.swap_promo_text),
                iconResId = R.drawable.img_okx_dex_logo,
                onCloseClick = {},
            ),
            NotificationConfig(
                title = resourceReference(R.string.swap_promo_title),
                subtitle = resourceReference(R.string.swap_promo_text),
                iconResId = R.drawable.img_okx_dex_logo,
                onCloseClick = {},
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = resourceReference(R.string.token_swap_promotion_button),
                    onClick = {},
                ),
            ),
        )
}
// endregion