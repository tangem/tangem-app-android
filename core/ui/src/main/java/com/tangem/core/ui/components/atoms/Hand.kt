package com.tangem.core.ui.components.atoms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme

/**
 * [Show in Figma](https://www.figma.com/file/17JyRbuUEZ42DluaFEuGQk/Atoms?node-id=135%3A25&t=Jo68S6ilyewVU8MV-4)
 * */
@Composable
fun Hand(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(vertical = TangemTheme.dimens.spacing8)
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

// region Preview
@Composable
private fun HandSample(
    modifier: Modifier = Modifier,
) {
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
@Composable
private fun HandPreview_Light() {
    TangemTheme {
        HandSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HandPreview_Dark() {
    TangemTheme(isDark = true) {
        HandSample()
    }
}
// endregion Preview
