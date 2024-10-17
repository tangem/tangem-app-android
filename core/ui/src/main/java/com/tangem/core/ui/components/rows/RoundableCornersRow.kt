package com.tangem.core.ui.components.rows

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import com.tangem.core.ui.R
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
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
    currentIndex: Int,
    lastIndex: Int,
    iconResId: Int? = null,
    iconClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .roundedShapeItemDecoration(
                currentIndex = currentIndex,
                lastIndex = lastIndex,
                addDefaultPadding = false,
            ),
        color = TangemTheme.colors.background.action,
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
                            indication = ripple(bounded = false, radius = TangemTheme.dimens.radius10),
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
                currentIndex = previewData.currentIndex,
                lastIndex = previewData.lastIndex,
                iconResId = previewData.iconResId,
            )
        }
    }
}

private data class RoundableCornersRowPreviewData(
    val startText: String,
    val endText: String,
    val currentIndex: Int,
    val lastIndex: Int,
    val iconResId: Int? = null,
)

private class RoundableCornersRowDataProvider :
    PreviewParameterProvider<RoundableCornersRowPreviewData> {

    override val values: Sequence<RoundableCornersRowPreviewData>
        get() = sequenceOf(
            getPreviewData(currentIndex = 1, lastIndex = 2),
            getPreviewData(currentIndex = 0, lastIndex = 2),
            getPreviewData(currentIndex = 2, lastIndex = 2),
            getPreviewData(currentIndex = 1, lastIndex = 2, iconResId = R.drawable.ic_alert_24),
            getPreviewData(currentIndex = 0, lastIndex = 2, iconResId = R.drawable.ic_alert_24),
            getPreviewData(currentIndex = 2, lastIndex = 2, iconResId = R.drawable.ic_alert_24),
        )

    private fun getPreviewData(currentIndex: Int, lastIndex: Int, iconResId: Int? = null) =
        RoundableCornersRowPreviewData(
            startText = "startText",
            endText = "endText",
            currentIndex = currentIndex,
            lastIndex = lastIndex,
            iconResId = iconResId,
        )
}