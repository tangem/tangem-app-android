package com.tangem.core.ui.components.marketprice

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun PriceChangeInPercent(
    valueInPercent: String,
    type: PriceChangeType,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false,
) {
    if (LocalRedesignEnabled.current) {
        PriceChangeInPercentV2(
            modifier = modifier,
            valueInPercent = valueInPercent,
            type = type,
            textStyle = textStyle,
            isDisabled = isDisabled,
        )
    } else {
        PriceChangeInPercentV1(
            modifier = modifier,
            valueInPercent = valueInPercent,
            type = type,
            textStyle = textStyle,
        )
    }
}

@Composable
private fun PriceChangeInPercentV1(
    valueInPercent: String,
    type: PriceChangeType,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    if (valueInPercent.isBlank()) {
        Box(modifier)
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing2),
    ) {
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens.size8)
                .align(Alignment.CenterVertically),
            imageVector = ImageVector.vectorResource(
                id = when (type) {
                    PriceChangeType.UP -> R.drawable.ic_arrow_up_8
                    PriceChangeType.DOWN -> R.drawable.ic_arrow_down_8
                    PriceChangeType.NEUTRAL -> R.drawable.ic_elipse_8
                },
            ),
            tint = when (type) {
                PriceChangeType.UP -> TangemTheme.colors.icon.accent
                PriceChangeType.DOWN -> TangemTheme.colors.icon.warning
                PriceChangeType.NEUTRAL -> TangemTheme.colors.icon.inactive
            },
            contentDescription = null,
        )

        Text(
            text = valueInPercent,
            color = when (type) {
                PriceChangeType.UP -> TangemTheme.colors.text.accent
                PriceChangeType.DOWN -> TangemTheme.colors.text.warning
                PriceChangeType.NEUTRAL -> TangemTheme.colors.text.disabled
            },
            style = textStyle,
            overflow = TextOverflow.Visible,
            maxLines = 1,
        )
    }
}

@Composable
private fun PriceChangeInPercentV2(
    valueInPercent: String,
    type: PriceChangeType,
    textStyle: TextStyle,
    isDisabled: Boolean,
    modifier: Modifier = Modifier,
) {
    if (valueInPercent.isBlank()) {
        Box(modifier)
        return
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens2.x0_5),
    ) {
        Icon(
            modifier = Modifier
                .size(TangemTheme.dimens2.x3)
                .align(Alignment.CenterVertically),
            imageVector = ImageVector.vectorResource(
                id = when (type) {
                    PriceChangeType.UP -> R.drawable.ic_arrow_up_8
                    PriceChangeType.DOWN -> R.drawable.ic_arrow_down_8
                    PriceChangeType.NEUTRAL -> R.drawable.ic_elipse_8
                },
            ),
            tint = if (isDisabled) {
                TangemTheme.colors2.graphic.neutral.tertiary
            } else {
                when (type) {
                    PriceChangeType.UP -> TangemTheme.colors2.markers.iconBlue
                    PriceChangeType.DOWN -> TangemTheme.colors2.markers.iconRed
                    PriceChangeType.NEUTRAL -> TangemTheme.colors2.markers.iconGray
                }
            },
            contentDescription = null,
        )

        Text(
            text = valueInPercent,
            color = if (isDisabled) {
                TangemTheme.colors2.text.status.disabled
            } else {
                when (type) {
                    PriceChangeType.UP -> TangemTheme.colors2.text.status.accent
                    PriceChangeType.DOWN -> TangemTheme.colors2.text.status.warning
                    PriceChangeType.NEUTRAL -> TangemTheme.colors2.text.neutral.tertiary
                }
            },
            style = textStyle,
            overflow = TextOverflow.Visible,
            maxLines = 1,
        )
    }
}

//region Preview

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV1() {
    TangemThemePreview {
        Column {
            PriceChangeInPercent(
                valueInPercent = "52.00%",
                type = PriceChangeType.NEUTRAL,
                textStyle = TangemTheme.typography.body2,
            )
            PriceChangeInPercent(
                valueInPercent = "52.00%",
                type = PriceChangeType.UP,
                textStyle = TangemTheme.typography.body2,
            )
            PriceChangeInPercent(
                valueInPercent = "52.00%",
                type = PriceChangeType.DOWN,
                textStyle = TangemTheme.typography.body2,
            )
            PriceChangeInPercent(
                valueInPercent = "52.00%",
                type = PriceChangeType.DOWN,
                textStyle = TangemTheme.typography.caption2,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV2() {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            Column {
                PriceChangeInPercent(
                    valueInPercent = "52.00%",
                    type = PriceChangeType.NEUTRAL,
                    textStyle = TangemTheme.typography2.captionRegular12,
                )
                PriceChangeInPercent(
                    valueInPercent = "52.00%",
                    type = PriceChangeType.UP,
                    textStyle = TangemTheme.typography2.captionRegular12,
                    isDisabled = true,
                )
                PriceChangeInPercent(
                    valueInPercent = "52.00%",
                    type = PriceChangeType.DOWN,
                    textStyle = TangemTheme.typography2.captionRegular12,
                    isDisabled = false,
                )
                PriceChangeInPercent(
                    valueInPercent = "52.00%",
                    type = PriceChangeType.DOWN,
                    textStyle = TangemTheme.typography2.captionRegular12,
                )
            }
        }
    }
}

//endregion Preview