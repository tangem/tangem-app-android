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

private var _ic_bell_24: ImageVector? = null

val Icons.ic_bell_24: ImageVector
    get() {
        if (_ic_bell_24 != null) return _ic_bell_24!!
        _ic_bell_24 = ImageVector.Builder(
            name = "ic_bell_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 1.99902C15.866 1.99917 18.9999 5.13314 19 8.99902V12.6318L20.0723 13.7031C20.666 14.2981 20.9993 15.1048 21 15.9453C20.9998 17.6313 19.6333 18.999 17.9473 18.999H14.9668C14.8834 19.6925 14.6261 20.361 14.1943 20.8984C13.6572 21.5668 12.875 22.0002 12 22C11.1254 22.0005 10.3438 21.5674 9.80664 20.8994C9.37441 20.3618 9.11566 19.6929 9.03223 18.999H6.05176C5.24226 18.9989 4.466 18.6769 3.89355 18.1045C3.32116 17.5321 2.99913 16.7558 2.99902 15.9463C2.99974 15.1058 3.334 14.2981 3.92773 13.7031L4.99902 12.6318V8.99902C4.99912 5.13311 8.13394 1.99911 12 1.99902ZM11.0576 18.999C11.12 19.254 11.229 19.477 11.3652 19.6465C11.5779 19.9108 11.8138 20.0011 11.999 20.001L12 20.6416L12.001 20.001C12.1864 20.0009 12.4221 19.9101 12.6348 19.6455C12.7708 19.4762 12.879 19.2536 12.9414 18.999H11.0576ZM12 3.99902C9.23833 3.99911 6.99912 6.23786 6.99902 8.99902V13.0469C6.99902 13.3121 6.89362 13.5664 6.70605 13.7539L5.3418 15.1172C5.14984 15.31 5.03163 15.5627 5.00488 15.8311L4.99902 15.9463C4.99913 16.2253 5.11032 16.4931 5.30762 16.6904C5.50497 16.8877 5.77266 16.9989 6.05176 16.999H17.9473C18.4926 16.999 18.9411 16.5845 18.9951 16.0537L19 15.9463C18.9996 15.6355 18.8766 15.3374 18.6572 15.1172L17.293 13.7539C17.1056 13.5664 17 13.3119 17 13.0469V8.99902C16.9999 6.2379 14.7616 3.99917 12 3.99902Z"),
            )
        }.build()
        return _ic_bell_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBell24Preview() {
    Icon(
        imageVector = Icons.ic_bell_24,
        contentDescription = null,
    )
}