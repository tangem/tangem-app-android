package com.tangem.feature.referral.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

@Composable
fun AwardText(
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
        color = TangemTheme.colors.background.primary
    ) {
        Column {
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

                Row {
                    Text(
                        text = endText,
                        color = endTextColor,
                        maxLines = 1,
                        style = endTextStyle,
                    )
                }
            }
        }
    }
}


@Preview(widthDp = 360, showBackground = true)
@Composable
fun Preview_AwardItem_Light() {
    TangemTheme {
        AwardText(
            startText = "startText",
            startTextColor = TangemTheme.colors.text.tertiary,
            startTextStyle = TangemTheme.typography.subtitle2,
            endText = "endText",
            endTextColor = TangemTheme.colors.text.primary1,
            endTextStyle = TangemTheme.typography.subtitle2,
            cornersToRound = CornersToRound.TOP_2
        )
    }

}

enum class CornersToRound {
    ALL_4,
    TOP_2,
    BOTTOM_2,
    ZERO;

    @Composable
    fun getShape(): RoundedCornerShape {
        val radius = TangemTheme.dimens.radius12
        return when (this) {
            ALL_4 -> RoundedCornerShape(radius)
            TOP_2 -> RoundedCornerShape(topStart = radius, topEnd = radius)
            BOTTOM_2 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
            ZERO -> RoundedCornerShape(0.dp)
        }
    }
}