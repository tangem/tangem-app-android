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

private var _ic_earth_28: ImageVector? = null

val Icons.ic_earth_28: ImageVector
    get() {
        if (_ic_earth_28 != null) return _ic_earth_28!!
        _ic_earth_28 = ImageVector.Builder(
            name = "ic_earth_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2C20.6411 2 25.9999 7.35891 26 14C26 20.6411 20.6411 26 14 26L13.6895 25.9961C7.19242 25.8323 1.99902 20.5374 1.99902 14C1.99909 7.35895 7.35895 2.00007 14 2ZM13.5098 4.5127C12.1368 4.58189 10.8433 4.94203 9.68652 5.53027L11.5059 7.34961C12.3734 8.21732 12.8613 9.39523 12.8613 10.6221C12.8613 13.178 10.7892 15.2497 8.2334 15.25H4.58301C5.1921 19.9159 9.16359 23.4999 14 23.5C16.2195 23.5 18.2549 22.7423 19.8691 21.4746L18.3535 19.2002C18.2195 18.9992 18.0059 18.8693 17.7715 18.8398L17.6699 18.834L17.4346 18.8252C16.2695 18.7429 15.2268 18.0518 14.6992 16.9971L14.54 16.6797C14.0727 15.745 14.0728 14.6437 14.54 13.709L15.8926 11.0029C16.4547 9.87669 17.6071 9.16699 18.8643 9.16699H22.1846C20.5342 6.37058 17.4931 4.5 14 4.5L13.5098 4.5127ZM18.8643 11.667C18.5521 11.667 18.2677 11.8433 18.1299 12.1191L18.1289 12.1201L16.7764 14.8271C16.661 15.0581 16.6609 15.3306 16.7764 15.5615L16.9346 15.8789C17.0743 16.1578 17.3592 16.334 17.6699 16.334C18.7812 16.3343 19.8178 16.8898 20.4336 17.8135L21.6514 19.6406C22.8132 18.0647 23.5 16.1154 23.5 14C23.5 13.1945 23.4002 12.4131 23.2129 11.667H18.8643ZM7.59473 6.97363C5.98047 8.4452 4.88087 10.4685 4.58301 12.75H8.2334C9.40851 12.7497 10.3613 11.7973 10.3613 10.6221C10.3613 10.0575 10.1365 9.51553 9.73828 9.11719L7.59473 6.97363Z"),
            )
        }.build()
        return _ic_earth_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcEarth28Preview() {
    Icon(
        imageVector = Icons.ic_earth_28,
        contentDescription = null,
    )
}