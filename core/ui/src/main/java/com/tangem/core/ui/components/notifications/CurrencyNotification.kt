package com.tangem.core.ui.components.notifications

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Currency notification component from Design system.
 *
 * @param config         component config
 * @param modifier       modifier
 * @param containerColor container color
 *
 * @see <a href = "https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=3251-3466&t=eZb7ZPoNE8pQw45Y-4"
 * >Figma component</a>
 */
@Composable
fun CurrencyNotification(
    config: CurrencyNotificationConfig,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
) {
    NotificationBaseContainer(
        buttonsState = config.buttonsState,
        onClick = null,
        onCloseClick = null,
        modifier = modifier,
        containerColor = containerColor,
    ) {
        MainContent(
            tokenIconState = config.tokenIconState,
            title = config.title,
            subtitle = config.subtitle,
        )
    }
}

@Composable
private fun MainContent(
    tokenIconState: CurrencyIconState,
    title: TextReference,
    subtitle: CurrencyNotificationConfig.AnnotatedSubtitle,
) {
    Row {
        CurrencyIcon(
            state = tokenIconState,
            modifier = Modifier.align(alignment = Alignment.CenterVertically),
        )

        SpacerW(width = TangemTheme.dimens.spacing6)

        TextsBlock(title = title, subtitle = subtitle)
    }
}

@Composable
private fun TextsBlock(title: TextReference, subtitle: CurrencyNotificationConfig.AnnotatedSubtitle) {
    Column(verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing2)) {
        Text(
            text = title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.button,
        )

        val subtitleValue = subtitle.valueProvider()
        ClickableText(
            text = subtitleValue,
            onClick = { subtitle.onClick(subtitleValue, it) },
            style = TangemTheme.typography.caption2,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Notification() {
    TangemThemePreview {
        CurrencyNotification(
            config = CurrencyNotificationConfig(
                title = resourceReference(
                    R.string.express_exchange_notification_refund_title,
                    wrappedList("USDT", "Polygon"),
                ),
                subtitle = CurrencyNotificationConfig.AnnotatedSubtitle(
                    valueProvider = {
                        buildAnnotatedString {
                            append("Your transaction amount was refunded in USDT to your wallet due to OKX")
                        }
                    },
                    onClick = { _, _ -> },
                ),
                tokenIconState = CurrencyIconState.TokenIcon(
                    url = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/usd-coin.png",
                    topBadgeIconResId = R.drawable.ic_polygon_22,
                    isGrayscale = false,
                    showCustomBadge = false,
                    fallbackTint = Color(1.0f, 1.0f, 1.0f, 1.0f, ColorSpaces.Srgb),
                    fallbackBackground = Color(0.23529412f, 0.28627452f, 0.6117647f, 1.0f, ColorSpaces.Srgb),
                ),
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = stringReference("Go to token"),
                    onClick = {},
                ),
            ),
        )
    }
}
