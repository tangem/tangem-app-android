package com.tangem.core.ui.components.bottomsheets.sheet

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.TangemTopAppBarHeight
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun TangemBottomSheetTitle(
    title: TextReference?,
    modifier: Modifier = Modifier,
    endButton: TopAppBarButtonUM? = null,
    containerColor: Color = Color.Transparent,
) {
    TangemBottomSheetTitle(
        modifier = modifier,
        title = title?.resolveReference(),
        endButton = endButton,
        containerColor = containerColor,
    )
}

@Composable
fun TangemBottomSheetTitle(
    title: String?,
    modifier: Modifier = Modifier,
    endButton: TopAppBarButtonUM? = null,
    containerColor: Color = Color.Transparent,
) {
    TangemTopAppBar(
        modifier = modifier,
        title = title,
        endButton = endButton,
        textColor = TangemTheme.colors.text.primary1,
        iconTint = TangemTheme.colors.icon.informative,
        titleAlignment = Alignment.CenterHorizontally,
        containerColor = containerColor,
        height = TangemTopAppBarHeight.BOTTOM_SHEET,
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TangemBottomSheetTitle() {
    TangemThemePreview {
        TangemBottomSheetTitle(
            title = "Title",
            endButton = TopAppBarButtonUM.Icon(
                iconRes = R.drawable.ic_information_24,
                onClicked = {},
            ),
            containerColor = TangemTheme.colors.background.secondary,
        )
    }
}
// endregion Preview