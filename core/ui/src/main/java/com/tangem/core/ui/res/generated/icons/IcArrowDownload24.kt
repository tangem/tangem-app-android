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

private var _ic_arrow_download_24: ImageVector? = null

val Icons.ic_arrow_download_24: ImageVector
    get() {
        if (_ic_arrow_download_24 != null) return _ic_arrow_download_24!!
        _ic_arrow_download_24 = ImageVector.Builder(
            name = "ic_arrow_download_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20 18.501C20.5523 18.501 21 18.9487 21 19.501C20.9999 20.0532 20.5522 20.501 20 20.501H4C3.44777 20.501 3.00009 20.0532 3 19.501C3 18.9487 3.44772 18.501 4 18.501H20Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 3.5C12.5523 3.5 13 3.94772 13 4.5V14.0303L16.3594 11.2314C16.7836 10.878 17.415 10.9353 17.7686 11.3594C18.1221 11.7836 18.0648 12.415 17.6406 12.7686L12.6396 16.9355C12.6146 16.9565 12.588 16.9751 12.5615 16.9932C12.5553 16.9974 12.5493 17.0018 12.543 17.0059C12.5221 17.0194 12.501 17.0321 12.4795 17.0439C12.4754 17.0462 12.4709 17.0476 12.4668 17.0498C12.4434 17.0622 12.4196 17.0736 12.3955 17.084C12.3877 17.0873 12.3799 17.0906 12.3721 17.0938C12.3562 17.1001 12.3403 17.1058 12.3242 17.1113C12.2796 17.1267 12.2338 17.1395 12.1865 17.1484C12.1697 17.1516 12.1527 17.1529 12.1357 17.1553C12.101 17.16 12.066 17.164 12.0303 17.165C12.0078 17.1657 11.9854 17.1649 11.9629 17.1641C11.9302 17.1629 11.898 17.1605 11.8662 17.1562C11.8469 17.1537 11.8277 17.1512 11.8086 17.1475C11.7645 17.1389 11.7215 17.1274 11.6797 17.1133C11.5653 17.0747 11.4557 17.0167 11.3584 16.9355L6.3584 12.7686C5.93422 12.4149 5.87687 11.7836 6.23047 11.3594C6.58408 10.9352 7.21544 10.8779 7.63965 11.2314L11 14.0322V4.5C11 3.94772 11.4477 3.5 12 3.5Z"),
            )
        }.build()
        return _ic_arrow_download_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownload24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_download_24,
        contentDescription = null,
    )
}