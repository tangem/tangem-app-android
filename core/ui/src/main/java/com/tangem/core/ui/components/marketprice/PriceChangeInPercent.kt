package com.tangem.core.ui.components.marketprice

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun PriceChangeInPercent(
    valueInPercent: String,
    type: PriceChangeType,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TangemTheme.typography.body2,
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

//region Preview

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        Column {
            PriceChangeInPercent(
                valueInPercent = "52.00%",
                type = PriceChangeType.NEUTRAL,
            )
            PriceChangeInPercent(
                valueInPercent = "52.00%",
                type = PriceChangeType.UP,
            )
            PriceChangeInPercent(
                valueInPercent = "52.00%",
                type = PriceChangeType.DOWN,
            )
            PriceChangeInPercent(
                valueInPercent = "52.00%",
                type = PriceChangeType.DOWN,
                textStyle = TangemTheme.typography.caption2,
            )
        }
    }
}

//endregion Preview
