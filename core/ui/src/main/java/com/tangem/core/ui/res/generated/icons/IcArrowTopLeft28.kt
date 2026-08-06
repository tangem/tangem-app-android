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

private var _ic_arrow_top_left_28: ImageVector? = null

val Icons.ic_arrow_top_left_28: ImageVector
    get() {
        if (_ic_arrow_top_left_28 != null) return _ic_arrow_top_left_28!!
        _ic_arrow_top_left_28 = ImageVector.Builder(
            name = "ic_arrow_top_left_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.74872 4.5C6.05916 4.5007 5.49906 5.06036 5.49872 5.75V16.75C5.49903 17.4397 6.05915 17.9993 6.74872 18C7.43888 18 7.99841 17.4401 7.99872 16.75V8.91113L20.8366 22.6045C21.3085 23.1078 22.0995 23.1333 22.6032 22.6621C23.1067 22.19 23.1327 21.3992 22.6608 20.8955L9.63544 7H17.7487C18.4389 7 18.9984 6.44009 18.9987 5.75C18.9984 5.05993 18.4389 4.5 17.7487 4.5H6.74872Z"),
            )
        }.build()
        return _ic_arrow_top_left_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopLeft28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_left_28,
        contentDescription = null,
    )
}