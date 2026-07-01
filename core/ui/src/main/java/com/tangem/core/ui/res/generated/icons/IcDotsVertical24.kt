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

private var _ic_dots_vertical_24: ImageVector? = null

val Icons.ic_dots_vertical_24: ImageVector
    get() {
        if (_ic_dots_vertical_24 != null) return _ic_dots_vertical_24!!
        _ic_dots_vertical_24 = ImageVector.Builder(
            name = "ic_dots_vertical_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1025 16.5078C12.88 16.5619 13.4996 17.2066 13.4999 18.002C13.4999 18.8293 12.8272 19.5018 11.9999 19.502C11.1758 19.5018 10.5061 18.8347 10.5009 18.0117L10.4999 18.0127C10.4916 17.2114 11.1144 16.5629 11.8905 16.5078C11.9254 16.5041 11.9611 16.502 11.997 16.502C12.0325 16.502 12.0679 16.5042 12.1025 16.5078Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1025 10.5059C12.8801 10.56 13.4998 11.2045 13.4999 12C13.4998 12.8272 12.8271 13.4999 11.9999 13.5C11.1759 13.4999 10.5062 12.8326 10.5009 12.0098L10.4999 12.0107C10.4914 11.2093 11.1143 10.5609 11.8905 10.5059C11.9255 10.5022 11.9611 10.5 11.997 10.5C12.0325 10.5 12.0679 10.5022 12.1025 10.5059Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.1025 4.50293C12.8801 4.55704 13.4997 5.20171 13.4999 5.99707C13.4999 6.82439 12.8272 7.49695 11.9999 7.49707C11.1758 7.49694 10.5061 6.82977 10.5009 6.00684L10.4999 6.00781C10.4915 5.2065 11.1144 4.55797 11.8905 4.50293C11.9254 4.49924 11.9611 4.49708 11.997 4.49707C12.0325 4.49708 12.0679 4.49931 12.1025 4.50293Z"),
            )
        }.build()
        return _ic_dots_vertical_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDotsVertical24Preview() {
    Icon(
        imageVector = Icons.ic_dots_vertical_24,
        contentDescription = null,
    )
}