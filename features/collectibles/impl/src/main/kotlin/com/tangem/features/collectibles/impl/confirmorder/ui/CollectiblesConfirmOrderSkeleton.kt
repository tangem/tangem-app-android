package com.tangem.features.collectibles.impl.confirmorder.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.footer.TangemFooter
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
                repeat(PRICE_ROWS) { PriceRowSkeleton() }
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

@Composable
private fun PriceRowSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TangemShimmer(style = TangemTheme.typography3.body.medium, modifier = Modifier.width(120.dp))
        TangemShimmer(style = TangemTheme.typography3.body.medium, modifier = Modifier.width(64.dp))
    }
}