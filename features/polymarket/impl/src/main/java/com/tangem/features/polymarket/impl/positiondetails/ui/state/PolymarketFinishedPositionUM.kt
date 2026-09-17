package com.tangem.features.polymarket.impl.positiondetails.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class PolymarketFinishedPositionUM(
    val summary: PolymarketPositionDetailsUM.Summary,
    val shouldShowOutcome: Boolean,
    val resolutionDate: TextReference? = null,
    val closureDate: TextReference? = null,
)