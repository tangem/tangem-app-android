package com.tangem.core.ui.components.appbar

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * App bar with back button and optional title
 *
 * @param text        optional title
 * @param onBackClick the lambda to be invoked when this icon is pressed
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=123%3A287&t=WdN5XpixzZLlQAZO-4"
 * >Figma component</a>
 */
@Composable
fun AppBarWithBackButton(
    text: String? = null,
    @DrawableRes iconRes: Int? = null,
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.primary)
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.spacing16)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing16)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes ?: R.drawable.ic_back_24),
            contentDescription = null,
            modifier = Modifier
                .size(size = dimensionResource(R.dimen.size24))
                .clickable { onBackClick() },
            tint = MaterialTheme.colors.onPrimary,
        )
        if (!text.isNullOrBlank()) {
            Text(
                text = text,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
                style = MaterialTheme.typography.subtitle1,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
fun PreviewAppBarWithBackButtonInLightTheme() {
    TangemTheme(isDark = false) {
        AppBarWithBackButton(text = "Title", onBackClick = {})
    }
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Composable
fun PreviewAppBarWithBackButtonInDarkTheme() {
    TangemTheme(isDark = true) {
        AppBarWithBackButton(text = "Title", onBackClick = {})
    }
}
