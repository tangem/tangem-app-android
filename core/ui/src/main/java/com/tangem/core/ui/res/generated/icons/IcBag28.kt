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

private var _ic_bag_28: ImageVector? = null

val Icons.ic_bag_28: ImageVector
    get() {
        if (_ic_bag_28 != null) return _ic_bag_28!!
        _ic_bag_28 = ImageVector.Builder(
            name = "ic_bag_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.417 10.4719C18.107 10.4722 18.6669 11.0318 18.667 11.7219V12.8606C18.667 15.3571 16.7065 17.3963 14.2412 17.5217L14.001 17.5276L13.7607 17.5217C11.2949 17.3969 9.33398 15.3575 9.33398 12.8606V11.7219C9.33412 11.0317 9.89371 10.4719 10.584 10.4719C11.274 10.4722 11.8339 11.0318 11.834 11.7219V12.8606C11.834 14.0572 12.8044 15.0276 14.001 15.0276C15.1972 15.0271 16.167 14.0569 16.167 12.8606V11.7219C16.1671 11.0318 16.7269 10.4721 17.417 10.4719Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.833 2.50024C22.7812 2.50024 24.3601 4.07945 24.3604 6.02759V6.84888C25.0607 7.49349 25.4999 8.4178 25.5 9.44458V21.9729C25.4998 23.921 23.9199 25.5002 21.9717 25.5002H6.02734C4.07934 25.5 2.50022 23.9209 2.5 21.9729V9.44458C2.50012 8.41798 2.93864 7.49347 3.63867 6.84888V6.02759C3.6389 4.07967 5.21813 2.50061 7.16602 2.50024H20.833ZM6.02734 8.41724C5.46006 8.41747 5.00023 8.8773 5 9.44458V21.9729C5.00022 22.5402 5.46006 23 6.02734 23.0002H21.9717C22.5392 23.0002 22.9998 22.5403 23 21.9729V9.44458C22.9998 8.87715 22.5392 8.41724 21.9717 8.41724H6.02734ZM7.16602 5.00024C6.63615 5.00058 6.20077 5.40196 6.14551 5.91724H21.8545C21.7993 5.40172 21.3632 5.00024 20.833 5.00024H7.16602Z"),
            )
        }.build()
        return _ic_bag_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBag28Preview() {
    Icon(
        imageVector = Icons.ic_bag_28,
        contentDescription = null,
    )
}