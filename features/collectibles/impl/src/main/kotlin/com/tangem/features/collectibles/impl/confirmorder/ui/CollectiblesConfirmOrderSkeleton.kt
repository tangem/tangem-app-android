package com.tangem.features.collectibles.impl.confirmorder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme

private const val PRICE_ROWS = 3

/** Mirrors [ConfirmOrderContent]: the pack in the same frame and the price rows above the footer. */
@Composable
internal fun CollectiblesConfirmOrderSkeleton(footerHeight: Int, modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val packWidth = with(density) { (constraints.maxWidth * PACK_WIDTH_FRACTION).toDp() }

            TangemShimmer(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -PACK_LIFT)
                    .size(width = packWidth, height = packWidth * PACK_ASPECT),
                radius = 20.dp,
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp),
            ) {
                repeat(PRICE_ROWS) { index -> PriceRowSkeleton(hasDivider = index < PRICE_ROWS - 1) }
                Spacer(modifier = Modifier.height(with(density) { footerHeight.toDp() }))
            }
        }
    }
}

@Composable
internal fun CollectiblesConfirmOrderFooterSkeleton(modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        TangemFooter(modifier = modifier) {
            TangemShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                radius = 24.dp,
            )
        }
    }
}

/** Laid out by [TangemRow] like the real rows, so the dividers and paddings match; only the total has none. */
@Composable
private fun PriceRowSkeleton(hasDivider: Boolean) {
    TangemRow(
        divider = hasDivider,
        titleSlot = { TextLineShimmer(width = 120.dp) },
        valueSlot = { TextLineShimmer(width = 72.dp) },
    )
}

@Composable
private fun TextLineShimmer(width: Dp) {
    val lineHeight = with(LocalDensity.current) { TangemTheme.typography3.body.medium.lineHeight.toDp() }
    TangemShimmer(modifier = Modifier.size(width = width, height = lineHeight), radius = 16.dp)
}