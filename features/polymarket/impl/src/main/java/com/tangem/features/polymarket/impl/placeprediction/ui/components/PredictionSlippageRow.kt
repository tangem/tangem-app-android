package com.tangem.features.polymarket.impl.placeprediction.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_double_vertical_16
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.features.polymarket.impl.placeprediction.entity.DEFAULT_SLIPPAGE_PERCENT
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.label
import java.math.BigDecimal

/**
 * The slippage line of the place-prediction flow.
 */
@Composable
internal fun PredictionSlippageRow(slippage: SlippageUM, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    TangemRow(
        modifier = modifier,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResourceSafe(R.string.prediction_place_slippage),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                Icon(
                    imageVector = Icons.ic_info_16,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.tertiary,
                )
            }
        },
        valueSlot = {
            Text(
                text = slippage.label().resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        },
        endSlot = if (onClick != null) {
            {
                Icon(
                    imageVector = Icons.ic_chevron_double_vertical_16,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.tertiary,
                )
            }
        } else {
            null
        },
        onClick = onClick,
    )
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PredictionSlippageRowPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            PreviewSurface {
                PredictionSlippageRow(
                    slippage = SlippageUM(percent = DEFAULT_SLIPPAGE_PERCENT, isDefault = true),
                    onClick = {},
                )
            }
            PreviewSurface {
                PredictionSlippageRow(
                    slippage = SlippageUM(percent = DEFAULT_SLIPPAGE_PERCENT, isDefault = true),
                )
            }
            PreviewSurface {
                PredictionSlippageRow(
                    slippage = SlippageUM(percent = BigDecimal("0.25"), isDefault = false),
                    onClick = {},
                )
            }
            PreviewSurface {
                PredictionSlippageRow(
                    slippage = SlippageUM(percent = BigDecimal("0.25"), isDefault = false),
                )
            }
        }
    }
}

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    TangemSurface(color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape, content = content)
}