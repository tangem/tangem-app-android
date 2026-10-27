package com.tangem.features.polymarket.impl.placeprediction.ui.components

import android.content.res.Configuration
import androidx.annotation.StringRes
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape

@Composable
internal fun PredictionTotalRow(
    value: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    @StringRes titleRes: Int = R.string.prediction_fee_sheet_total,
    divider: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    TangemRow(
        modifier = modifier,
        divider = divider,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = { TotalTitle(titleRes = titleRes) },
        valueSlot = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TangemBadge(
                    text = stringReference(PolymarketConst.USD_CODE),
                    size = TangemBadge.Size.X6,
                )
                if (isLoading) {
                    TangemShimmer(style = TangemTheme.typography3.body.medium, textAlign = TextAlign.End)
                } else {
                    Text(
                        text = value,
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                }
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun TotalTitle(@StringRes titleRes: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResourceSafe(titleRes),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Icon(
            imageVector = Icons.ic_info_16,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.tertiary,
        )
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PredictionTotalRowPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            TangemSurface(color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape) {
                PredictionTotalRow(value = "$1.40", isLoading = false, onClick = {})
            }
            TangemSurface(color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape) {
                PredictionTotalRow(value = "", isLoading = true)
            }
        }
    }
}