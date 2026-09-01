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

private var _ic_user_16: ImageVector? = null

val Icons.ic_user_16: ImageVector
    get() {
        if (_ic_user_16 != null) return _ic_user_16!!
        _ic_user_16 = ImageVector.Builder(
            name = "ic_user_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.39551 9.09683C11.1176 9.09691 12.4275 10.4802 12.4277 12.1291C12.4277 12.4742 12.1479 12.7541 11.8027 12.7541C11.4576 12.754 11.1777 12.4742 11.1777 12.1291C11.1775 11.152 10.4088 10.3469 9.39551 10.3468H6.60449C5.62744 10.347 4.82251 11.1159 4.82227 12.1291C4.82227 12.4742 4.54244 12.7541 4.19727 12.7541C3.85209 12.7541 3.57227 12.4742 3.57227 12.1291C3.57251 10.407 4.95571 9.09699 6.60449 9.09683H9.39551Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.95703 3.57632C7.12783 2.47459 8.92591 2.47454 10.0967 3.57632C10.0991 3.57864 10.1011 3.58179 10.1035 3.58413C10.1056 3.58614 10.1083 3.58794 10.1104 3.58999C11.2767 4.75656 11.2965 6.64028 10.0967 7.76968C8.95175 8.84721 7.07475 8.95988 5.92969 7.74331C4.85222 6.59837 4.74048 4.72136 5.95703 3.57632ZM9.23047 4.47769C8.54089 3.83777 7.4998 3.84063 6.81348 4.48647C6.1698 5.09234 6.1675 6.17146 6.84082 6.88687C7.44675 7.53007 8.525 7.53264 9.24023 6.85952C9.89904 6.23924 9.91998 5.1715 9.23047 4.47769Z"),
            )
        }.build()
        return _ic_user_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUser16Preview() {
    Icon(
        imageVector = Icons.ic_user_16,
        contentDescription = null,
    )
}