package com.tangem.core.ui.components.appbar

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.res.TangemThemePreview

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
    TangemTopAppBar(
        modifier = modifier,
        title = text,
        startButton = TopAppBarButtonUM(
            iconRes = iconRes ?: R.drawable.ic_back_24,
            onIconClicked = onBackClick,
        ),
    )
}

// region Preview
@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Preview(widthDp = 360, heightDp = 56, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppBarWithBackButton() {
    TangemThemePreview {
        AppBarWithBackButton(text = "Title", onBackClick = {})
    }
}
// endregion