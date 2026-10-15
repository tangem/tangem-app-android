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

private var _ic_rose_24: ImageVector? = null

val Icons.ic_rose_24: ImageVector
    get() {
        if (_ic_rose_24 != null) return _ic_rose_24!!
        _ic_rose_24 = ImageVector.Builder(
            name = "ic_rose_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13 2C14.9003 2 16.4897 3.32509 16.8975 5.10156C18.6745 5.50892 20 7.09932 20 9C20 10.4803 19.1951 11.7712 18 12.4629V13C18 15.9725 15.8373 18.4387 13 18.915V21C13 21.5523 12.5523 22 12 22C11.4477 22 11 21.5523 11 21V18.915C8.16273 18.4387 6 15.9725 6 13V11.7422C4.77867 10.7333 4 9.20762 4 7.5C4 4.46272 6.46272 2 9.5 2H13ZM8 13C8 15.2087 9.79128 17 12 17C14.2087 17 16 15.2087 16 13H9.5C8.97982 13 8.47699 12.926 8 12.791V13ZM9.5 4C7.56728 4 6 5.56728 6 7.5C6 8.70693 6.61132 9.77115 7.54102 10.4004C7.54929 10.4057 7.55732 10.4114 7.56543 10.417C8.11965 10.7853 8.78479 11 9.5 11H16C17.1047 11 18 10.1047 18 9C18 8.18906 17.5168 7.49265 16.8232 7.17871C16.3202 8.81257 14.799 10 13 10H10.0781C8.69784 10 7.57812 8.88028 7.57812 7.5C7.57812 6.11972 8.69784 5 10.0781 5H12.1084C12.6605 5.00022 13.1084 5.44785 13.1084 6C13.1084 6.55215 12.6605 6.99978 12.1084 7H10.0781C9.80241 7 9.57812 7.22428 9.57812 7.5C9.57812 7.77572 9.80241 8 10.0781 8H13C14.1047 8 15 7.10472 15 6C15 4.89528 14.1047 4 13 4H9.5Z"),
            )
        }.build()
        return _ic_rose_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcRose24Preview() {
    Icon(
        imageVector = Icons.ic_rose_24,
        contentDescription = null,
    )
}