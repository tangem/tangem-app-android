package com.tangem.core.ui.components.appbar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.models.AdditionalButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

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
    text: TextReference,
    modifier: Modifier = Modifier,
    startButton: AdditionalButton? = null,
    endButton: AdditionalButton? = null,
    textColor: Color = TangemTheme.colors.text.primary1,
    iconColor: Color = TangemTheme.colors.icon.primary1,
) {
    AppBarWithAdditionalButtons(
        text = text.resolveReference(),
        startButton = startButton,
        endButton = endButton,
        modifier = modifier,
        textColor = textColor,
        iconColor = iconColor,
    )
}

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
    modifier: Modifier = Modifier,
    startButton: AdditionalButton? = null,
    endButton: AdditionalButton? = null,
    textColor: Color = TangemTheme.colors.text.primary1,
    iconColor: Color = TangemTheme.colors.icon.primary1,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = TangemTheme.dimens.spacing16),
    ) {
        if (startButton != null) {
            Icon(
                painter = painterResource(id = startButton.iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(TangemTheme.dimens.size24)
                    .align(Alignment.CenterStart)
                    .clickable(
                        onClick = startButton.onIconClicked,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material.ripple.rememberRipple(
                            bounded = false,
                            radius = TangemTheme.dimens.size24 / 2,
                        ),
                    ),
            )
        }

        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center),
            color = textColor,
            maxLines = 1,
            style = TangemTheme.typography.subtitle1,
        )

        if (endButton != null) {
            Icon(
                painter = painterResource(id = endButton.iconRes),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(TangemTheme.dimens.size24)
                    .align(Alignment.CenterEnd)
                    .clickable(
                        onClick = endButton.onIconClicked,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = androidx.compose.material.ripple.rememberRipple(
                            bounded = false,
                            radius = TangemTheme.dimens.size24 / 2,
                        ),
                    ),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Preview(widthDp = 360, heightDp = 56, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AppBarWithAdditionalButtons() {
    TangemThemePreview {
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
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Preview(widthDp = 360, heightDp = 56, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AppBarWithOnlyStartButtons() {
    TangemThemePreview {
        AppBarWithAdditionalButtons(
            text = "Tangem",
            startButton = AdditionalButton(
                iconRes = R.drawable.ic_scan_24,
                onIconClicked = {},
            ),
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
        )
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Preview(widthDp = 360, heightDp = 56, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AppBarWithOnlyEndButtons() {
    TangemThemePreview {
        AppBarWithAdditionalButtons(
            text = "Tangem",
            endButton = AdditionalButton(
                iconRes = R.drawable.ic_more_vertical_24,
                onIconClicked = {},
            ),
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
        )
    }
}