package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
internal sealed interface SharesProceedsUM {

    data object Unavailable : SharesProceedsUM

    data object Loading : SharesProceedsUM

    data class Content(val expected: BigDecimal) : SharesProceedsUM
}