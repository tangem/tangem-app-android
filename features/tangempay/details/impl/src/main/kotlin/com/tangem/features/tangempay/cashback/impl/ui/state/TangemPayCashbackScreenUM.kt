package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.compose.runtime.Immutable

/** Top-level state of the Cashback screen. The top navigation (title + close) is shown in every state. */
@Immutable
internal sealed interface TangemPayCashbackScreenUM {

    val onCloseClick: () -> Unit

    /** Skeleton placeholder shown while the screen data is loading. */
    data class Loading(
        override val onCloseClick: () -> Unit,
    ) : TangemPayCashbackScreenUM

    /** The page failed to load; the user can tap to retry. */
    data class Error(
        override val onCloseClick: () -> Unit,
        val onReloadClick: () -> Unit,
    ) : TangemPayCashbackScreenUM

    /** Loaded content. Each section is nullable and hidden when its data is unavailable. */
    data class Content(
        override val onCloseClick: () -> Unit,
        val cashback: TangemPayCashbackUM,
        val infoTiles: TangemPayCashbackInfoTilesUM?,
        val histogram: TangemPayCashbackHistogramUM?,
        val additionalCashback: TangemPayAdditionalCashbackUM?,
    ) : TangemPayCashbackScreenUM
}