package com.tangem.core.ui.components.appbar

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

/**
 * App bar with back button and optional title
 *
 * @param onBackClick the lambda to be invoked when this icon is pressed
 * @param modifier    modifier
 * @param text        optional title
 * @param iconRes     icon res id
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=123%3A287&t=WdN5XpixzZLlQAZO-4"
 * >Figma component</a>
 */
@Composable
fun AppBarWithBackButton(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    @DrawableRes iconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth()
            .padding(all = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes ?: R.drawable.ic_back_24),
            contentDescription = null,
            modifier = Modifier
                .size(size = TangemTheme.dimens.size24)
                .clickable(
                    indication = rememberRipple(bounded = false),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onBackClick,
                ),
            tint = TangemTheme.colors.icon.primary1,
        )
        if (!text.isNullOrBlank()) {
            Text(
                text = text,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                style = TangemTheme.typography.subtitle1,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Preview(widthDp = 360, heightDp = 56, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppBarWithBackButton() {
    TangemThemePreview {
        AppBarWithBackButton(text = "Title", onBackClick = {})
    }
}
