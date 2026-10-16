@file:Suppress("all")

package com.tangem.core.ui.res.generated.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _ic_chair_28: ImageVector? = null

val Icons.ic_chair_28: ImageVector
    get() {
        if (_ic_chair_28 != null) return _ic_chair_28!!
        _ic_chair_28 = ImageVector.Builder(
            name = "ic_chair_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.3338 3C20.8191 3.00016 22.8338 5.01469 22.8338 7.5V11.8838C23.4011 12.0985 23.9073 12.4661 24.2899 12.9561C24.9249 13.7696 25.1496 14.8308 24.8993 15.832L23.7958 20.2461C23.6339 20.8929 23.292 21.4562 22.8338 21.8916V23.75C22.8338 24.4402 22.274 24.9997 21.5838 25C20.8935 25 20.3338 24.4404 20.3338 23.75V22.834H7.66588V23.75C7.66588 24.4401 7.10588 24.9996 6.41588 25C5.72552 25 5.16588 24.4404 5.16588 23.75V21.8926C4.70731 21.4571 4.36597 20.8932 4.20396 20.2461L3.10045 15.832C2.85008 14.8307 3.07462 13.7696 3.70982 12.9561C4.09253 12.4659 4.59834 12.0985 5.16588 11.8838V7.5C5.16588 5.01471 7.18069 3.00018 9.66588 3H18.3338ZM21.6473 14.167C21.2563 14.1671 20.9159 14.4334 20.8211 14.8125L20.1356 17.5537C19.9963 18.1098 19.4959 18.4998 18.9227 18.5H9.07701C8.50368 18.5 8.00349 18.1098 7.86412 17.5537L7.17857 14.8125C7.08382 14.4335 6.74324 14.1673 6.3524 14.167C6.08993 14.167 5.842 14.2883 5.68052 14.4951C5.51923 14.7019 5.46166 14.9712 5.52525 15.2256L6.62877 19.6396C6.73108 20.0472 7.09789 20.334 7.51842 20.334H20.4813C20.9017 20.3338 21.2687 20.0472 21.371 19.6396L22.4745 15.2256C22.538 14.9714 22.4803 14.7018 22.3192 14.4951C22.1578 14.2884 21.9097 14.1671 21.6473 14.167ZM9.66588 5.5C8.56115 5.50018 7.66588 6.39567 7.66588 7.5V11.9355C8.60919 12.338 9.34358 13.163 9.60435 14.2061L10.0526 16H17.9471L18.3954 14.2061C18.6562 13.1628 19.3902 12.3379 20.3338 11.9355V7.5C20.3338 6.39565 19.4386 5.50016 18.3338 5.5H9.66588Z"),
            )
        }.build()
        return _ic_chair_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChair28Preview() {
    Icon(
        imageVector = Icons.ic_chair_28,
        contentDescription = null,
    )
}