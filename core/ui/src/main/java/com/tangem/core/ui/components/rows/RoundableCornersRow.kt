package com.tangem.core.ui.components.rows

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Suppress("LongParameterList")
@Composable
fun RoundableCornersRow(
    startText: String,
    startTextColor: Color,
    startTextStyle: TextStyle,
    endText: String,
    endTextColor: Color,
    endTextStyle: TextStyle,
    cornersToRound: CornersToRound,
    iconResId: Int? = null,
    iconClick: (() -> Unit)? = null,
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
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = startText,
                color = startTextColor,
                maxLines = 1,
                style = startTextStyle,
            )
            if (iconResId != null && iconClick != null) {
                Icon(
                    modifier = Modifier
                        .padding(TangemTheme.dimens.spacing4)
                        .size(TangemTheme.dimens.size16)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false, radius = TangemTheme.dimens.radius10),
                            onClick = iconClick,
                        ),
                    painter = painterResource(id = R.drawable.ic_alert_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.text.tertiary,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = endText,
                color = endTextColor,
                maxLines = 1,
                style = endTextStyle,
            )
        }
    }
}

enum class CornersToRound {

    ALL_4,
    TOP_2,
    BOTTOM_2,
    ZERO,
    ;

    @Suppress("TopLevelComposableFunctions")
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

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_RoundableCornersRow(
    @PreviewParameter(RoundableCornersRowDataProvider::class) previewData: RoundableCornersRowPreviewData,
) {
    TangemThemePreview {
        Box(modifier = Modifier.background(color = TangemTheme.colors.icon.attention)) {
            RoundableCornersRow(
                startText = previewData.startText,
                startTextColor = TangemTheme.colors.text.tertiary,
                startTextStyle = TangemTheme.typography.subtitle2,
                endText = previewData.endText,
                endTextColor = TangemTheme.colors.text.primary1,
                endTextStyle = TangemTheme.typography.subtitle2,
                cornersToRound = previewData.cornersToRound,
                iconResId = previewData.iconResId,
            )
        }
    }
}

private data class RoundableCornersRowPreviewData(
    val startText: String,
    val endText: String,
    val cornersToRound: CornersToRound,
    val iconResId: Int? = null,
)

private class RoundableCornersRowDataProvider :
    PreviewParameterProvider<RoundableCornersRowPreviewData> {

    override val values: Sequence<RoundableCornersRowPreviewData>
        get() = sequenceOf(
            getPreviewData(cornersToRound = CornersToRound.ZERO),
            getPreviewData(cornersToRound = CornersToRound.TOP_2),
            getPreviewData(cornersToRound = CornersToRound.BOTTOM_2),
            getPreviewData(cornersToRound = CornersToRound.ZERO, iconResId = R.drawable.ic_alert_24),
            getPreviewData(cornersToRound = CornersToRound.TOP_2, iconResId = R.drawable.ic_alert_24),
            getPreviewData(cornersToRound = CornersToRound.BOTTOM_2, iconResId = R.drawable.ic_alert_24),
        )

    private fun getPreviewData(cornersToRound: CornersToRound, iconResId: Int? = null) = RoundableCornersRowPreviewData(
        startText = "startText",
        endText = "endText",
        cornersToRound = cornersToRound,
        iconResId = iconResId,
    )
}
