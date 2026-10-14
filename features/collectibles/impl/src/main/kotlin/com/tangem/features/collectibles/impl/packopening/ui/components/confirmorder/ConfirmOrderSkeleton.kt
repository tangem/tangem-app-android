package com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
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

@Composable
internal fun ConfirmOrderSkeleton(contentPadding: PaddingValues) {
    ProvideTangemShimmer {
        ConfirmOrderLayout(
            contentPadding = contentPadding,
            pack = { packModifier -> TangemShimmer(modifier = packModifier, radius = 20.dp) },
            rows = {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    repeat(PRICE_ROWS) { index -> PriceRowSkeleton(hasDivider = index < PRICE_ROWS - 1) }
                }
            },
            footer = {
                TangemFooter(fadeEnabled = false, blurBackground = false) {
                    TangemShimmer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        radius = 24.dp,
                    )
                }
            },
        )
    }
}

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