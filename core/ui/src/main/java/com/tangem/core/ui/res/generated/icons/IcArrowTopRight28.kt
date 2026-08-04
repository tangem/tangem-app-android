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

private var _ic_arrow_top_right_28: ImageVector? = null

val Icons.ic_arrow_top_right_28: ImageVector
    get() {
        if (_ic_arrow_top_right_28 != null) return _ic_arrow_top_right_28!!
        _ic_arrow_top_right_28 = ImageVector.Builder(
            name = "ic_arrow_top_right_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.2505 4.5C21.9404 4.50039 22.5003 5.06005 22.5005 5.75V16.75C22.5003 17.44 21.9404 17.9996 21.2505 18C20.5603 17.9999 20.0006 17.4401 20.0005 16.75V8.91113L7.16257 22.6045C6.69057 23.1077 5.89957 23.1336 5.39596 22.6621C4.89258 22.1901 4.86672 21.3991 5.33835 20.8955L18.3637 7H10.2505C9.56033 6.99986 9.00062 6.44013 9.00046 5.75C9.00065 5.0599 9.56034 4.50014 10.2505 4.5H21.2505Z"),
            )
        }.build()
        return _ic_arrow_top_right_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopRight28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_right_28,
        contentDescription = null,
    )
}