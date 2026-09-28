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

private var _ic_logo_tangem_28: ImageVector? = null

val Icons.ic_logo_tangem_28: ImageVector
    get() {
        if (_ic_logo_tangem_28 != null) return _ic_logo_tangem_28!!
        _ic_logo_tangem_28 = ImageVector.Builder(
            name = "ic_logo_tangem_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.9992 13.5996V24H9.26646C7.77281 24 7.02637 23.9997 6.45522 23.7412C5.95354 23.5151 5.54738 23.152 5.29129 22.7061C5.00053 22.1984 5 21.535 5 20.2073V13.5996H10.9992Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23 20.2073C23 21.535 22.9995 22.1984 22.7087 22.7061C22.4526 23.152 22.0464 23.5151 21.5448 23.7412C20.9736 23.9997 20.2272 24 18.7335 24H17.0008V14.3381L16.9996 13.5996H23V20.2073Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.7335 4C20.2272 4 20.9736 4.00033 21.5448 4.25879C22.0465 4.48493 22.4526 4.84798 22.7087 5.29395C22.9995 5.80164 23 6.46503 23 7.79272V9.2002H5V7.79272C5 6.46503 5.00053 5.80164 5.29129 5.29395C5.54565 4.84798 5.95354 4.48647 6.45522 4.25879C7.02637 4.00033 7.77281 4 9.26646 4H18.7335Z"),
            )
        }.build()
        return _ic_logo_tangem_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLogoTangem28Preview() {
    Icon(
        imageVector = Icons.ic_logo_tangem_28,
        contentDescription = null,
    )
}