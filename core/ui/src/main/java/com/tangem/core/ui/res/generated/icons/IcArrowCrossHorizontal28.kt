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

private var _ic_arrow_cross_horizontal_28: ImageVector? = null

val Icons.ic_arrow_cross_horizontal_28: ImageVector
    get() {
        if (_ic_arrow_cross_horizontal_28 != null) return _ic_arrow_cross_horizontal_28!!
        _ic_arrow_cross_horizontal_28 = ImageVector.Builder(
            name = "ic_arrow_cross_horizontal_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.7217 15.9511C14.2713 15.5336 15.055 15.6409 15.4727 16.1904L16.5479 17.6045C17.0522 18.268 17.7734 18.6425 18.5391 18.6425H21.7031L21.4834 18.4257C20.992 17.9409 20.9868 17.1496 21.4717 16.6582C21.9565 16.1668 22.7478 16.1607 23.2393 16.6455L25.6279 19.0029C25.8659 19.2377 25.9999 19.5582 26 19.8925C26 20.2269 25.8658 20.5473 25.6279 20.7822L23.2393 23.1396C22.7479 23.6244 21.9566 23.6192 21.4717 23.1279C20.9868 22.6365 20.992 21.8452 21.4834 21.3603L21.7041 21.1425H18.5391C16.9158 21.1425 15.4866 20.3393 14.5576 19.1172L13.4824 17.7031C13.0647 17.1535 13.1721 16.3689 13.7217 15.9511ZM21.4717 4.87204C21.9566 4.38079 22.7479 4.37549 23.2393 4.86032L25.6279 7.21774C25.8659 7.45261 26 7.77306 26 8.10739C25.9999 8.44171 25.8659 8.76222 25.6279 8.99704L23.2393 11.3545C22.7478 11.8392 21.9565 11.8331 21.4717 11.3418C20.9869 10.8504 20.992 10.059 21.4834 9.57419L21.7031 9.35739H18.5391C17.7734 9.35739 17.0522 9.73196 16.5479 10.3955L16.5449 10.4004L9.8584 19.1162C8.92948 20.3383 7.50109 21.1425 5.87793 21.1425H3.25C2.55964 21.1425 2 20.5829 2 19.8925C2.00016 19.2023 2.55974 18.6425 3.25 18.6425H5.87793C6.64354 18.6425 7.36479 18.268 7.86914 17.6045L7.87207 17.5996L14.5605 8.87887C15.4896 7.65893 16.9176 6.85739 18.5391 6.85739H21.7041L21.4834 6.63962C20.992 6.15474 20.9868 5.36345 21.4717 4.87204ZM5.87793 6.85739C7.50109 6.85744 8.93045 7.66067 9.85938 8.88278L10.9336 10.2968C11.3514 10.8465 11.2449 11.631 10.6953 12.0488C10.1457 12.4665 9.36111 12.3591 8.94336 11.8095L7.86914 10.3955C7.36479 9.73195 6.64354 9.35744 5.87793 9.35739H3.25C2.55974 9.35739 2.00015 8.79762 2 8.10739C2 7.41703 2.55964 6.85739 3.25 6.85739H5.87793Z"),
            )
        }.build()
        return _ic_arrow_cross_horizontal_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCrossHorizontal28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_cross_horizontal_28,
        contentDescription = null,
    )
}