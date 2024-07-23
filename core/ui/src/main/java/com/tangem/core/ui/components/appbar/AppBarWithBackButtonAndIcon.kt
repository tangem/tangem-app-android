package com.tangem.core.ui.components.appbar

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun AppBarWithBackButtonAndIcon(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    subtitle: String? = null,
    @DrawableRes backIconRes: Int? = null,
    @DrawableRes iconRes: Int? = null,
    onIconClick: (() -> Unit)? = null,
    backgroundColor: Color = TangemTheme.colors.background.secondary,
) {
    TangemTopAppBar(
        modifier = modifier,
        title = text,
        subtitle = subtitle,
        containerColor = backgroundColor,
        startButton = TopAppBarButtonUM(
            iconRes = backIconRes ?: R.drawable.ic_back_24,
            onIconClicked = onBackClick,
        ),
        endButton = if (iconRes != null && onIconClick != null) {
            TopAppBarButtonUM(
                iconRes = iconRes,
                onIconClicked = onIconClick,
            )
        } else {
            null
        },
    )
}

@Preview(widthDp = 360, heightDp = 56, showBackground = true)
@Preview(widthDp = 360, heightDp = 56, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppBarWithBackButtonAndIcon() {
    TangemThemePreview {
        AppBarWithBackButtonAndIcon(
            text = "Title",
            iconRes = R.drawable.ic_qrcode_scan_24,
            onBackClick = {},
            onIconClick = {},
        )
    }
}