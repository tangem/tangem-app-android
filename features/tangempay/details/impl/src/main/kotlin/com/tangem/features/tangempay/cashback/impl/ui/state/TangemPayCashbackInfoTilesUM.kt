package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.TextReference

@Immutable
data class TangemPayCashbackInfoTilesUM(
    val rate: Tile,
    val accruals: Tile,
) {

    @Immutable
    data class Tile(
        val icon: ImageVector,
        val title: TextReference,
        val subtitle: TextReference,
        val onClick: () -> Unit,
    )
}