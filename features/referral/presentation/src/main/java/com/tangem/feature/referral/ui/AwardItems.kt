package com.tangem.feature.referral.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Suppress("LongParameterList")
@Composable
internal fun AwardText(
    startText: String,
    startTextColor: Color,
    startTextStyle: TextStyle,
    endText: String,
    endTextColor: Color,
    endTextStyle: TextStyle,
    cornersToRound: CornersToRound,
) {
    Surface(
        shape = cornersToRound.getShape(),
        color = TangemTheme.colors.background.primary,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(TangemTheme.dimens.size48)
                .padding(
                    horizontal = TangemTheme.dimens.spacing16,
                    vertical = TangemTheme.dimens.spacing12,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = startText,
                color = startTextColor,
                maxLines = 1,
                style = startTextStyle,
            )

            Text(
                text = endText,
                color = endTextColor,
                maxLines = 1,
                style = endTextStyle,
            )
        }
    }
}

@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview_AwardItem() {
    TangemThemePreview {
        AwardText(
            startText = "startText",
            startTextColor = TangemTheme.colors.text.tertiary,
            startTextStyle = TangemTheme.typography.subtitle2,
            endText = "endText",
            endTextColor = TangemTheme.colors.text.primary1,
            endTextStyle = TangemTheme.typography.subtitle2,
            cornersToRound = CornersToRound.TOP_2,
        )
    }
}
