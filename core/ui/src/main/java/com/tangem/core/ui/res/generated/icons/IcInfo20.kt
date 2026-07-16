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

private var _ic_info_20: ImageVector? = null

val Icons.ic_info_20: ImageVector
    get() {
        if (_ic_info_20 != null) return _ic_info_20!!
        _ic_info_20 = ImageVector.Builder(
            name = "ic_info_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99805 9.24805C10.4122 9.24816 10.748 9.5839 10.748 9.99805V13.6221C10.748 14.0362 10.4122 14.372 9.99805 14.3721C9.58396 14.3719 9.24805 14.0362 9.24805 13.6221V10.748H8.96289C8.54868 10.748 8.21289 10.4123 8.21289 9.99805C8.21289 9.58383 8.54868 9.24805 8.96289 9.24805H9.99805Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.88867 6.08105C10.342 6.12732 10.698 6.50964 10.6982 6.97754C10.6982 7.47513 10.2944 7.87868 9.79688 7.87891C9.33049 7.87874 8.94667 7.52453 8.90039 7.07031L8.89551 6.97754C8.89487 6.47743 9.30195 6.07617 9.79688 6.07617L9.88867 6.08105ZM9.69629 7.4668L9.79688 7.47656C9.76151 7.47656 9.72634 7.4709 9.69238 7.46582L9.69629 7.4668Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99805 2C14.4156 2 17.9961 5.58049 17.9961 9.99805C17.996 14.4156 14.4156 17.9961 9.99805 17.9961C5.58054 17.996 2.00005 14.4156 2 9.99805C2.00002 5.58052 5.58052 2.00004 9.99805 2ZM9.99805 3.5C6.40895 3.50004 3.50003 6.40895 3.5 9.99805C3.50005 13.5871 6.40896 16.496 9.99805 16.4961C13.5872 16.4961 16.496 13.5872 16.4961 9.99805C16.4961 6.40892 13.5872 3.5 9.99805 3.5Z"),
            )
        }.build()
        return _ic_info_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcInfo20Preview() {
    Icon(
        imageVector = Icons.ic_info_20,
        contentDescription = null,
    )
}