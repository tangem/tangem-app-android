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

private var _ic_bag_32: ImageVector? = null

val Icons.ic_bag_32: ImageVector
    get() {
        if (_ic_bag_32 != null) return _ic_bag_32!!
        _ic_bag_32 = ImageVector.Builder(
            name = "ic_bag_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.834 11.9443C20.6621 11.9448 21.334 12.6162 21.334 13.4443V14.7217C21.334 17.5751 19.0923 19.9059 16.2744 20.0488L16 20.0557L15.7256 20.0488C12.9078 19.9058 10.667 17.5751 10.667 14.7217V13.4443C10.667 12.6159 11.3386 11.9443 12.167 11.9443C12.9952 11.9446 13.667 12.6161 13.667 13.4443V14.7217C13.667 16.0102 14.7115 17.0555 16 17.0557C17.2885 17.0554 18.334 16.0102 18.334 14.7217V13.4443C18.334 12.6159 19.0056 11.9443 19.834 11.9443Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.667 3C25.9066 3.00026 27.7227 4.816 27.7227 7.05566V7.93555C28.5086 8.67518 28.9999 9.72433 29 10.8887V24.9443C28.9998 27.184 27.184 29 24.9443 29H7.05566C4.81596 29 3.00018 27.184 3 24.9443V10.8887C3.00007 9.72383 3.4918 8.67425 4.27832 7.93457V7.05566C4.27832 4.81584 6.09417 3 8.33398 3H23.667ZM7.05566 9.83301C6.47276 9.83301 6.00013 10.3058 6 10.8887V24.9443C6.00018 25.5271 6.47279 26 7.05566 26H24.9443C25.5272 26 25.9998 25.5271 26 24.9443V10.8887C25.9999 10.3058 25.5272 9.83301 24.9443 9.83301H7.05566ZM8.33398 6C7.82741 6 7.40402 6.357 7.30176 6.83301H24.6982C24.596 6.35715 24.1733 6.00023 23.667 6H8.33398Z"),
            )
        }.build()
        return _ic_bag_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBag32Preview() {
    Icon(
        imageVector = Icons.ic_bag_32,
        contentDescription = null,
    )
}