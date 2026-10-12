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

private var _ic_users_28_filled: ImageVector? = null

val Icons.ic_users_28_filled: ImageVector
    get() {
        if (_ic_users_28_filled != null) return _ic_users_28_filled!!
        _ic_users_28_filled = ImageVector.Builder(
            name = "ic_users_28_filled",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.33594 4.96484C11.9362 4.96494 14.1267 7.0061 14.127 9.62305C14.127 12.2402 11.9364 14.2811 9.33594 14.2812C6.73559 14.2811 4.54492 12.2401 4.54492 9.62305C4.54515 7.00616 6.73573 4.96504 9.33594 4.96484Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.0732 6.42773C21.2538 6.42801 23.1035 8.14145 23.1035 10.3545C23.1033 12.5674 21.2537 14.281 19.0732 14.2812C16.8925 14.2812 15.0422 12.5676 15.042 10.3545C15.042 8.14127 16.8924 6.42773 19.0732 6.42773Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.24902 23.0352C2.55895 23.035 1.99927 22.4752 1.99902 21.7852V20.792C1.99906 17.9088 4.41317 15.6525 7.29004 15.6523H11.3828C12.5412 15.6524 13.6245 16.0181 14.5054 16.6433C15.3863 16.0181 16.4696 15.6524 17.6279 15.6523H18.7468H19.5918H20.71C23.5869 15.6524 26.0009 17.9087 26.001 20.792V21.7852C26.0007 22.4753 25.4411 23.0351 24.751 23.0352H3.24902Z"),
            )
        }.build()
        return _ic_users_28_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUsers28FilledPreview() {
    Icon(
        imageVector = Icons.ic_users_28_filled,
        contentDescription = null,
    )
}