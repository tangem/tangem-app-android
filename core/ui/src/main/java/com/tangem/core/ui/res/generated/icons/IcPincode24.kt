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

private var _ic_pincode_24: ImageVector? = null

val Icons.ic_pincode_24: ImageVector
    get() {
        if (_ic_pincode_24 != null) return _ic_pincode_24!!
        _ic_pincode_24 = ImageVector.Builder(
            name = "ic_pincode_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.002 8.49902C16.554 8.49929 17.002 8.9469 17.002 9.49902V14.501C17.0019 15.053 16.554 15.5007 16.002 15.501C15.4498 15.5009 15.002 15.0531 15.002 14.501V9.49902C15.002 8.94681 15.4498 8.49914 16.002 8.49902Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.71484 10.9326C7.25469 10.493 8.05043 10.5248 8.55371 11.0273C8.94733 11.4205 9.06523 12.0123 8.85254 12.5264C8.6397 13.0402 8.13825 13.375 7.58203 13.375C7.02584 13.3749 6.5243 13.0402 6.31152 12.5264C6.09886 12.0124 6.21682 11.4205 6.61035 11.0273L6.71484 10.9326Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.1328 10.9326C11.6727 10.4931 12.4684 10.5248 12.9717 11.0273C13.3652 11.4205 13.4831 12.0124 13.2705 12.5264C13.0577 13.0401 12.5561 13.3749 12 13.375C11.4438 13.3749 10.9423 13.0402 10.7295 12.5264C10.5168 12.0124 10.6348 11.4205 11.0283 11.0273L11.1328 10.9326Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2ZM12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4Z"),
            )
        }.build()
        return _ic_pincode_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPincode24Preview() {
    Icon(
        imageVector = Icons.ic_pincode_24,
        contentDescription = null,
    )
}