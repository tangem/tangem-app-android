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

private var _ic_search_24: ImageVector? = null

val Icons.ic_search_24: ImageVector
    get() {
        if (_ic_search_24 != null) return _ic_search_24!!
        _ic_search_24 = ImageVector.Builder(
            name = "ic_search_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11 3C15.4183 3 19 6.58172 19 11C19 12.8486 18.3703 14.5487 17.3174 15.9033L20.707 19.293C21.0976 19.6835 21.0976 20.3165 20.707 20.707C20.3165 21.0976 19.6835 21.0976 19.293 20.707L15.9033 17.3174C14.5487 18.3703 12.8486 19 11 19C6.58172 19 3 15.4183 3 11C3 6.58172 6.58172 3 11 3ZM11 5C7.68629 5 5 7.68629 5 11C5 14.3137 7.68629 17 11 17C14.3137 17 17 14.3137 17 11C17 7.68629 14.3137 5 11 5Z"),
            )
        }.build()
        return _ic_search_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSearch24Preview() {
    Icon(
        imageVector = Icons.ic_search_24,
        contentDescription = null,
    )
}