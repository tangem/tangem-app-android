package com.tangem.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_SimpleInfoCard_InLightTheme() {
    TangemTheme(isDark = false) {
        SmallInfoCard(startText = "Balance", endText = "0.4405434 BTC")
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
fun Preview_SimpleInfoCard_InDarkTheme() {
    TangemTheme(isDark = true) {
        SmallInfoCard(startText = "Balance", endText = "0.4405434 BTC")
    }
}

/**
 * Small card with text information attached to the edges
 *
 * @param startText text information attached to the left edge
 * @param endText   text information attached to the right edge
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=290%3A339&t=WdN5XpixzZLlQAZO-4"
 * >Figma component</a>
 */
@Composable
fun SmallInfoCard(startText: String, endText: String) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        color = TangemTheme.colors.background.secondary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TangemTheme.dimens.size48)
                .padding(
                    horizontal = TangemTheme.dimens.spacing16,
                    vertical = TangemTheme.dimens.spacing12,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = startText,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                style = TangemTheme.typography.subtitle2,
            )
            Text(
                text = endText,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                style = TangemTheme.typography.body2,
            )
        }
    }
}
