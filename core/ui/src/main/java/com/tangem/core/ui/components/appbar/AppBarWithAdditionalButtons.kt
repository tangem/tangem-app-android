package com.tangem.core.ui.components.appbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.models.AdditionalButton
import com.tangem.core.ui.res.IconColorType
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TextColorType
import com.tangem.core.ui.res.iconColor
import com.tangem.core.ui.res.textColor

/**
 * App bar with title and two additional buttons
 *
 * @param startButton button information attached to the left edge
 * @param endButton   button information attached to the right edge
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=50%3A403&t=tRQ4KlzkjV7TCLZl-4">
 * Figma Component</a>
 */
@Composable
fun AppBarWithAdditionalButtons(
    text: String,
    startButton: AdditionalButton? = null,
    endButton: AdditionalButton? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size56)
            .padding(all = TangemTheme.dimens.spacing16),
    ) {
        if (startButton != null) {
            IconButton(modifier = Modifier.align(Alignment.CenterStart), onClick = startButton.onIconClicked) {
                Icon(
                    painter = painterResource(id = startButton.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    tint = MaterialTheme.colors.iconColor(type = IconColorType.PRIMARY1),
                )
            }
        }

        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colors.textColor(type = TextColorType.PRIMARY1),
            maxLines = 1,
            style = MaterialTheme.typography.subtitle1,
        )

        if (endButton != null) {
            IconButton(modifier = Modifier.align(Alignment.CenterEnd), onClick = endButton.onIconClicked) {
                Icon(
                    painter = painterResource(id = endButton.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    tint = MaterialTheme.colors.iconColor(type = IconColorType.PRIMARY1),
                )
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
fun Preview_AppBarWithAdditionalButtons_InLightTheme() {
    TangemTheme(isDark = false) {
        AppBarWithAdditionalButtons(
            text = "Tangem",
            startButton = AdditionalButton(
                iconRes = R.drawable.ic_scan_24,
                onIconClicked = {},
            ),
            endButton = AdditionalButton(
                iconRes = R.drawable.ic_more_vertical_24,
                onIconClicked = {},
            ),
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
fun Preview_AppBarWithAdditionalButtons_InDarkTheme() {
    TangemTheme(isDark = true) {
        AppBarWithAdditionalButtons(
            text = "Tangem",
            startButton = AdditionalButton(
                iconRes = R.drawable.ic_scan_24,
                onIconClicked = {},
            ),
            endButton = AdditionalButton(
                iconRes = R.drawable.ic_more_vertical_24,
                onIconClicked = {},
            ),
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
fun Preview_AppBarWithOnlyStartButtons_InLightTheme() {
    TangemTheme(isDark = false) {
        AppBarWithAdditionalButtons(
            text = "Tangem",
            startButton = AdditionalButton(
                iconRes = R.drawable.ic_scan_24,
                onIconClicked = {},
            ),
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
fun Preview_AppBarWithOnlyStartButtons_InDarkTheme() {
    TangemTheme(isDark = true) {
        AppBarWithAdditionalButtons(
            text = "Tangem",
            startButton = AdditionalButton(
                iconRes = R.drawable.ic_scan_24,
                onIconClicked = {},
            ),
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
fun Preview_AppBarWithOnlyEndButtons_InLightTheme() {
    TangemTheme(isDark = false) {
        AppBarWithAdditionalButtons(
            text = "Tangem",
            endButton = AdditionalButton(
                iconRes = R.drawable.ic_more_vertical_24,
                onIconClicked = {},
            ),
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
fun Preview_AppBarWithOnlyEndButtons_InDarkTheme() {
    TangemTheme(isDark = true) {
        AppBarWithAdditionalButtons(
            text = "Tangem",
            endButton = AdditionalButton(
                iconRes = R.drawable.ic_more_vertical_24,
                onIconClicked = {},
            ),
        )
    }
}
