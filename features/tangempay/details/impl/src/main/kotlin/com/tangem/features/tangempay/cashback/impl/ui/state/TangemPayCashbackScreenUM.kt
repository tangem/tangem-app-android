package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayCashbackScreenUM(
    val cashback: TangemPayCashbackUM,
    val infoTiles: TangemPayCashbackInfoTilesUM?,
    val histogram: TangemPayCashbackHistogramUM?,
)