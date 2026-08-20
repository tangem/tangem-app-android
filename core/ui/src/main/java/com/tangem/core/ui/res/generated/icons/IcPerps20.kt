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

private var _ic_perps_20: ImageVector? = null

val Icons.ic_perps_20: ImageVector
    get() {
        if (_ic_perps_20 != null) return _ic_perps_20!!
        _ic_perps_20 = ImageVector.Builder(
            name = "ic_perps_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.6494 8.29004C11.5937 7.34622 13.1245 7.34633 14.0684 8.29102C15.0129 9.23557 15.0139 10.7664 14.0693 11.7109C13.1249 12.6553 11.594 12.655 10.6494 11.7109L10 11.0605L9.35156 11.71C8.40731 12.6542 6.87568 12.6538 5.93164 11.709C4.98709 10.7644 4.98611 9.23362 5.93066 8.28906C6.81605 7.40368 8.21706 7.34859 9.16699 8.12305L9.35156 8.28906L10 8.93848L10.6494 8.29004ZM13.0078 9.35059C12.6501 8.99261 12.0689 8.99267 11.71 9.35156L11.0605 10L11.709 10.6494C12.0677 11.0082 12.649 11.0081 13.0078 10.6494C13.3666 10.2906 13.3666 9.70935 13.0078 9.35059ZM8.29004 9.34961C7.93123 8.99129 7.3508 8.99197 6.99219 9.35059C6.63342 9.70935 6.63342 10.2897 6.99219 10.6484C7.34991 11.0064 7.93115 11.0073 8.29004 10.6484L8.93945 9.99902L8.29004 9.34961Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.001 2C14.4187 2.00046 18 5.58319 18 10.001C17.9995 14.4184 14.4184 17.9995 10.001 18C5.58319 18 2.00046 14.4187 2 10.001C2 5.5829 5.5829 2 10.001 2ZM10.001 3.50098C6.41133 3.50098 3.50098 6.41133 3.50098 10.001C3.50144 13.5902 6.41161 16.5 10.001 16.5C13.5899 16.4995 16.4995 13.5899 16.5 10.001C16.5 6.41161 13.5902 3.50144 10.001 3.50098Z"),
            )
        }.build()
        return _ic_perps_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcPerps20Preview() {
    Icon(
        imageVector = Icons.ic_perps_20,
        contentDescription = null,
    )
}