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

private var _ic_success_28: ImageVector? = null

val Icons.ic_success_28: ImageVector
    get() {
        if (_ic_success_28 != null) return _ic_success_28!!
        _ic_success_28 = ImageVector.Builder(
            name = "ic_success_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.2754 10.2539C17.7634 9.76614 18.5548 9.76631 19.043 10.2539C19.5309 10.742 19.5308 11.5333 19.043 12.0215L13.374 17.6924C13.3298 17.7579 13.2796 17.8209 13.2217 17.8789C12.7641 18.3363 12.0397 18.365 11.5488 17.9648L11.4531 17.8789L8.86621 15.29C8.37857 14.8018 8.37829 14.0104 8.86621 13.5225C9.35432 13.0347 10.1457 13.0347 10.6338 13.5225L12.3203 15.21L17.2754 10.2539Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2C20.6273 2.00026 26 7.37405 26 14.002C25.9998 20.6297 20.6272 26.0027 14 26.0029C7.37255 26.0029 2.00023 20.6299 2 14.002C2.00003 7.37389 7.37244 2 14 2ZM14 4.5C8.75348 4.5 4.50003 8.75426 4.5 14.002C4.50023 19.2495 8.7536 23.5029 14 23.5029C19.2462 23.5027 23.4998 19.2493 23.5 14.002C23.5 8.75442 19.2463 4.50026 14 4.5Z"),
            )
        }.build()
        return _ic_success_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSuccess28Preview() {
    Icon(
        imageVector = Icons.ic_success_28,
        contentDescription = null,
    )
}