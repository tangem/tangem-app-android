package com.tangem.core.ui.res

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class TangemShapes internal constructor(
    val roundedCornersSmall: Shape,
    val roundedCornersSmall2: Shape,
    val roundedCorners8: Shape,
    val roundedCornersMedium: Shape,
    val roundedCornersXMedium: Shape,
    val roundedCornersLarge: Shape,
    val roundedCornersXLarge: Shape,
    val bottomSheet: Shape,
    val bottomSheetLarge: Shape,
) {
    constructor() : this(
        roundedCornersSmall = RoundedCornerShape(size = 2.dp),
        roundedCornersSmall2 = RoundedCornerShape(size = 4.dp),
        roundedCorners8 = RoundedCornerShape(size = 8.dp),
        roundedCornersMedium = RoundedCornerShape(size = 12.dp),
        roundedCornersXMedium = RoundedCornerShape(size = 16.dp),
        roundedCornersLarge = RoundedCornerShape(size = 28.dp),
        roundedCornersXLarge = RoundedCornerShape(size = 36.dp),
        bottomSheet = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
        ),
        bottomSheetLarge = RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
        ),
    )
}