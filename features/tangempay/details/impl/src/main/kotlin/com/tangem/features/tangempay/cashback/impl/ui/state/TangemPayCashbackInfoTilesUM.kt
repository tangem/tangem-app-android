package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
data class TangemPayCashbackInfoTilesUM(
    val rate: Tile,
    val accruals: Tile,
) {

    @Immutable
    data class Tile(
        @DrawableRes val iconRes: Int,
        val title: TextReference,
        val subtitle: TextReference,
        val onClick: () -> Unit,
    )
}