package com.tangem.features.tangempay.cashback.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_20
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackInfoTilesUM

@Composable
internal fun TangemPayCashbackInfoTiles(state: TangemPayCashbackInfoTilesUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Tile(
            tile = state.rate,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        Tile(
            tile = state.accruals,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun Tile(tile: TangemPayCashbackInfoTilesUM.Tile, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier,
        color = TangemTheme.colors3.bg.secondary,
        shape = RoundedCornerShape(24.dp),
        onClick = tile.onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .heightIn(min = 100.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(TangemTheme.colors3.bg.opaque.primary)
                    .padding(6.dp),
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = rememberVectorPainter(tile.icon),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                )
            }
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = tile.title.resolveReference(),
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                Text(
                    text = tile.subtitle.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 402)
@Preview(showBackground = true, widthDp = 402, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCashbackInfoTilesPreview(
    @PreviewParameter(TangemPayCashbackInfoTilesPreviewProvider::class) state: TangemPayCashbackInfoTilesUM,
) {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(vertical = 16.dp),
        ) {
            TangemPayCashbackInfoTiles(state = state)
        }
    }
}

private class TangemPayCashbackInfoTilesPreviewProvider :
    CollectionPreviewParameterProvider<TangemPayCashbackInfoTilesUM>(
        listOf(
            TangemPayCashbackInfoTilesUM(
                rate = TangemPayCashbackInfoTilesUM.Tile(
                    icon = Icons.ic_percent_backward_20,
                    title = stringReference("Cashback 1%"),
                    subtitle = stringReference("With your Basic plan"),
                    onClick = {},
                ),
                accruals = TangemPayCashbackInfoTilesUM.Tile(
                    icon = Icons.ic_info_20,
                    title = stringReference("Accruals"),
                    subtitle = stringReference("Limits and exceptions"),
                    onClick = {},
                ),
            ),
            TangemPayCashbackInfoTilesUM(
                rate = TangemPayCashbackInfoTilesUM.Tile(
                    icon = Icons.ic_percent_backward_20,
                    title = stringReference("Cashback up to 2%"),
                    subtitle = stringReference("With your Plus plan"),
                    onClick = {},
                ),
                accruals = TangemPayCashbackInfoTilesUM.Tile(
                    icon = Icons.ic_info_20,
                    title = stringReference("Accruals"),
                    subtitle = stringReference("Limits and exceptions"),
                    onClick = {},
                ),
            ),
        ),
    )