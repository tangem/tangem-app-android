package com.tangem.core.ui.res

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape

@Immutable
data class TangemShapes internal constructor(
    val roundedCornersSmall: Shape,
    val roundedCornersSmall2: Shape,
    val roundedCorners8: Shape,
    val roundedCornersMedium: Shape,
    val roundedCornersXMedium: Shape,
    val roundedCornersLarge: Shape,
    val bottomSheet: Shape,
) {
    constructor(dimens: TangemDimens) : this(
        roundedCornersSmall = RoundedCornerShape(size = dimens.radius2),
        roundedCornersSmall2 = RoundedCornerShape(size = dimens.radius4),
        roundedCorners8 = RoundedCornerShape(size = dimens.radius8),
        roundedCornersMedium = RoundedCornerShape(size = dimens.radius12),
        roundedCornersXMedium = RoundedCornerShape(size = dimens.radius16),
        roundedCornersLarge = RoundedCornerShape(size = dimens.radius28),
        bottomSheet = RoundedCornerShape(
            topStart = dimens.radius16,
            topEnd = dimens.radius16,
        ),
    )
}
