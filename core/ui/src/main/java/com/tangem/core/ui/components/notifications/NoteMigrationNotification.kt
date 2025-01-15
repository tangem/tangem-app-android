package com.tangem.core.ui.components.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemColorPalette.Dark6
import com.tangem.core.ui.res.TangemColorPalette.Light2
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun NoteMigrationNotification(config: NotificationConfig, modifier: Modifier = Modifier) {
    val button = config.buttonsState as? NotificationConfig.ButtonsState.SecondaryButtonConfig
    button ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemColorPalette.Dark6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.heightIn(max = 146.dp),
            painter = painterResource(id = R.drawable.banner_note_migration),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            config.title?.let {
                Text(
                    text = it.resolveReference(),
                    color = TangemColorPalette.White,
                    style = TangemTheme.typography.h3,
                )
            }

            Text(
                text = config.subtitle.resolveReference(),
                textAlign = TextAlign.Center,
                color = TangemColorPalette.Light5,
                style = TangemTheme.typography.body2,
            )
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                text = button.text.resolveReference(),
                icon = TangemButtonIconPosition.None,
                onClick = button.onClick,
                colors = ButtonColors(
                    containerColor = Light2,
                    contentColor = Dark6,
                    disabledContainerColor = TangemTheme.colors.button.disabled,
                    disabledContentColor = TangemTheme.colors.text.disabled,
                ),
                enabled = true,
                textStyle = TangemTheme.typography.subtitle1,
                showProgress = false,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 328)
@Composable
private fun NoteMigrationNotification_Preview() {
    TangemThemePreview {
        NoteMigrationNotification(
            NotificationConfig(
                title = stringReference("Discover Tangem Wallet"),
                subtitle = stringReference(
                    "Access 13,000+ cryptocurrencies. Buy, sell, swap, and stake with a single tap. Link up to " +
                        "three cards for a backup.",
                ),
                iconResId = R.drawable.ic_empty_64,
                onCloseClick = { },
                buttonsState = NotificationConfig.ButtonsState.SecondaryButtonConfig(
                    text = stringReference("Get now"),
                    onClick = {},
                ),
            ),
        )
    }
}

// endregion