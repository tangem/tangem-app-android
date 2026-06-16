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

private var _ic_heart_broken_24: ImageVector? = null

val Icons.ic_heart_broken_24: ImageVector
    get() {
        if (_ic_heart_broken_24 != null) return _ic_heart_broken_24!!
        _ic_heart_broken_24 = ImageVector.Builder(
            name = "ic_heart_broken_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.7002 3C19.5684 3.00012 21.9999 6.58601 22 9.75977C22 11.4326 21.3441 12.9707 20.4434 14.2969C19.5418 15.6243 18.3575 16.7964 17.2002 17.7598C16.0389 18.7264 14.8765 19.5063 13.9951 20.0449C13.5539 20.3145 13.1785 20.5261 12.9033 20.6729C12.7665 20.7458 12.6494 20.8062 12.5586 20.8496C12.5147 20.8706 12.4679 20.8918 12.4238 20.9102C12.403 20.9188 12.3699 20.9316 12.332 20.9443C12.3134 20.9506 12.2814 20.9611 12.2422 20.9707C12.2183 20.9765 12.1232 21 12 21C11.8768 21 11.7817 20.9765 11.7578 20.9707C11.7186 20.9611 11.6866 20.9506 11.668 20.9443C11.6301 20.9316 11.597 20.9188 11.5762 20.9102C11.5321 20.8918 11.4853 20.8706 11.4414 20.8496C11.3506 20.8062 11.2335 20.7458 11.0967 20.6729C10.8215 20.5261 10.4461 20.3145 10.0049 20.0449C9.12346 19.5063 7.96108 18.7264 6.7998 17.7598C5.64249 16.7964 4.45822 15.6243 3.55664 14.2969C2.65593 12.9707 2 11.4326 2 9.75977C2.00012 6.58601 4.43162 3.00012 8.2998 3C9.93065 3 11.1475 3.61256 12 4.31543C12.8525 3.61256 14.0693 3 15.7002 3ZM8.2998 5C5.82837 5.00013 4.00012 7.37377 4 9.75977C4 10.9018 4.4493 12.0513 5.21094 13.1729C5.97178 14.2931 7.00748 15.3298 8.08008 16.2227C9.14873 17.1122 10.2268 17.8372 11.0479 18.3389C11.122 18.3842 11.1951 18.4254 11.2646 18.4668L12.3867 14.7275L10.2314 12.1406C9.92241 11.7698 9.92241 11.2302 10.2314 10.8594L12.3125 8.36133L11.377 6.49121C11.329 6.45293 11.2839 6.41048 11.2432 6.36328C10.6794 5.70965 9.74383 5 8.2998 5ZM15.7002 5C14.6411 5 13.856 5.38236 13.29 5.84375L14.3945 8.05273C14.5722 8.40809 14.5229 8.83541 14.2686 9.14062L12.3018 11.5L14.2686 13.8594C14.4833 14.1171 14.5543 14.4658 14.458 14.7871L13.4951 17.9951C14.2208 17.5263 15.0739 16.9269 15.9199 16.2227C16.9925 15.3298 18.0282 14.2931 18.7891 13.1729C19.5507 12.0513 20 10.9018 20 9.75977C19.9999 7.37377 18.1716 5.00013 15.7002 5Z"),
            )
        }.build()
        return _ic_heart_broken_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeartBroken24Preview() {
    Icon(
        imageVector = Icons.ic_heart_broken_24,
        contentDescription = null,
    )
}