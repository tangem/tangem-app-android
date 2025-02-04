package com.tangem.core.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

/**
 * Small card with an icon to the left and title with description shown to the right of it.
 * Additional context can be shown to the right of it.
 *
 * @param title shown to the right of icon
 * @param description shown to the right of icon
 * @param icon icon to the left
 * @param additionalContent shown at the right edge of the card
 *
 * @see <a href =
 * "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A4040&t=izokIIb9WWetO32R-1"
 * >Figma component</a>
 */
@Composable
fun CardWithIcon(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    additionalContent: @Composable () -> Unit = {},
) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        color = TangemTheme.colors.background.primary,
        tonalElevation = TangemTheme.dimens.elevation2,
        shadowElevation = TangemTheme.dimens.elevation2,
    ) {
        IconWithTitleAndDescription(
            title = title,
            description = description,
            icon = icon,
            additionalContent = additionalContent,
        )
    }
}

/**
 * A widget with an icon to the left and title with description shown to the right of it.
 * Additional context can be shown to the right of it.
 *
 * @param title shown to the right of icon
 * @param description shown to the right of icon
 * @param icon icon to the left
 * @param additionalContent shown at the right edge of the card
 *
 * @see <a href =
 * "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A4040&t=izokIIb9WWetO32R-1"
 * >Figma component</a>
 */
@Composable
internal fun IconWithTitleAndDescription(
    title: String,
    description: String?,
    icon: @Composable () -> Unit,
    additionalContent: @Composable () -> Unit = {},
    iconBackground: Color = TangemTheme.colors.background.secondary,
) {
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = iconBackground,
                    shape = CircleShape,
                )
                .height(TangemTheme.dimens.size40)
                .width(TangemTheme.dimens.size40),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        SpacerW12()

        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = true),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
            )
            if (description != null) {
                SpacerH4()
                Text(
                    text = description,
                    color = TangemTheme.colors.text.secondary,
                    style = TangemTheme.typography.body2,
                )
            }
        }

        additionalContent()
    }
}

// region Preview
@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_CardWithIcon() {
    TangemThemePreview {
        CardWithIcon(
            title = "Permit is valid until",
            description = "26:30",
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_clock_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.primary1,
                )
            },
        )
    }
}
// endregion Preview