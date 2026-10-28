package com.tangem.features.polymarket.impl.positiondetails.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.polymarket.impl.common.PolymarketResolutionSourceUM

@Immutable
internal data class PolymarketFinishedPositionUM(
    val summary: PolymarketPositionDetailsUM.Summary,
    val shouldShowOutcome: Boolean,
    val resolutionDate: TextReference? = null,
    val closureDate: TextReference? = null,
    val resolutionSource: PolymarketResolutionSourceUM? = null,
    val onResolutionSourceClick: (String) -> Unit = {},
)