package com.tangem.core.ui.components.atoms

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

/**
 * [Show in Figma](https://www.figma.com/file/17JyRbuUEZ42DluaFEuGQk/Atoms?node-id=135%3A25&t=Jo68S6ilyewVU8MV-4)
 * */
@Composable
fun Hand(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(handComposableComponentHeight)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(TangemTheme.dimens.size32)
                .height(TangemTheme.dimens.size4)
                .background(
                    color = TangemTheme.colors.icon.inactive,
                    shape = TangemTheme.shapes.roundedCornersSmall,
                ),
        )
    }
}

val handComposableComponentHeight: Dp
    @Composable
    @ReadOnlyComposable
    get() = TangemTheme.dimens.size4 + TangemTheme.dimens.spacing16

// region Preview
@Composable
private fun HandSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .padding(TangemTheme.dimens.spacing12),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Hand()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HandPreview() {
    TangemThemePreview {
        HandSample()
    }
}
// endregion Preview