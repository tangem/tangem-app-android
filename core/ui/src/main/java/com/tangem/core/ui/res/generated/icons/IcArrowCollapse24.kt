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

private var _ic_arrow_collapse_24: ImageVector? = null

val Icons.ic_arrow_collapse_24: ImageVector
    get() {
        if (_ic_arrow_collapse_24 != null) return _ic_arrow_collapse_24!!
        _ic_arrow_collapse_24 = ImageVector.Builder(
            name = "ic_arrow_collapse_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.5499 12.333C11.1662 12.3332 11.6667 12.833 11.6671 13.4492V17.7773C11.6671 18.3294 11.2191 18.777 10.6671 18.7773C10.1148 18.7773 9.66708 18.3296 9.66708 17.7773V15.7471L6.70712 18.707C6.31664 19.0973 5.68354 19.0973 5.29306 18.707C4.90259 18.3166 4.90269 17.6835 5.29306 17.293L8.25302 14.333H6.22274C5.67046 14.333 5.22274 13.8853 5.22274 13.333C5.22293 12.7809 5.67057 12.333 6.22274 12.333H10.5499Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.2931 5.29297C17.6835 4.90268 18.3166 4.90266 18.7071 5.29297C19.0972 5.68346 19.0974 6.3166 18.7071 6.70703L15.7472 9.66699H17.7774C18.3296 9.66718 18.7774 10.1148 18.7774 10.667C18.7773 11.219 18.3294 11.6668 17.7774 11.667H13.4503C12.8338 11.667 12.3335 11.1671 12.3331 10.5508V6.22265C12.3331 5.67048 12.781 5.22283 13.3331 5.22265C13.8852 5.22284 14.3331 5.67049 14.3331 6.22265V8.25293L17.2931 5.29297Z"),
            )
        }.build()
        return _ic_arrow_collapse_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowCollapse24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_collapse_24,
        contentDescription = null,
    )
}