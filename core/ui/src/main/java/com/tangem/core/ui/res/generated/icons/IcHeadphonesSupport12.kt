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

private var _ic_headphones_support_12: ImageVector? = null

val Icons.ic_headphones_support_12: ImageVector
    get() {
        if (_ic_headphones_support_12 != null) return _ic_headphones_support_12!!
        _ic_headphones_support_12 = ImageVector.Builder(
            name = "ic_headphones_support_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 1C7.98593 1 9.61947 2.52899 9.74121 4.47266C10.4505 4.58669 11 5.19356 11 5.93848V7.41797C10.9999 8.1937 10.4041 8.82107 9.65234 8.89648L9.5 8.9043C9.49614 9.72852 8.81977 10.3838 8 10.3838H7.31738C7.13134 10.7506 6.74864 11 6.3125 11H5.6875C5.07304 11 4.56277 10.5065 4.5625 9.88379C4.56262 9.261 5.07296 8.76758 5.6875 8.76758H6.3125C6.74889 8.76758 7.13146 9.01653 7.31738 9.38379H8C8.28279 9.38379 8.49986 9.15966 8.5 8.89746V8.76953C8.20314 8.59872 8.00007 8.28141 8 7.91113V5.44531C8 4.98235 8.31654 4.60107 8.73828 4.4873C8.6254 3.10058 7.45002 2 6 2C4.54995 2 3.37358 3.10055 3.26074 4.4873C3.68294 4.60077 4 4.98202 4 5.44531V7.91113C3.99989 8.46588 3.54555 8.9043 3 8.9043H2.5C1.67794 8.9043 1.0001 8.24543 1 7.41797V5.93848C1 5.19389 1.54897 4.58708 2.25781 4.47266C2.37953 2.52896 4.01405 1 6 1ZM5.6875 9.76758C5.61222 9.76758 5.56263 9.82621 5.5625 9.88379C5.56278 9.94128 5.61233 10 5.6875 10H6.3125C6.38767 10 6.43722 9.94128 6.4375 9.88379C6.43737 9.82621 6.38778 9.76758 6.3125 9.76758H5.6875ZM2.5 5.45215C2.21712 5.45215 2 5.67617 2 5.93848V7.41797C2.0001 7.6802 2.21719 7.9043 2.5 7.9043H3V5.45215H2.5ZM9 7.9043H9.5C9.78281 7.9043 9.9999 7.6802 10 7.41797V5.93848C10 5.67617 9.78288 5.45215 9.5 5.45215H9V7.9043Z"),
            )
        }.build()
        return _ic_headphones_support_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcHeadphonesSupport12Preview() {
    Icon(
        imageVector = Icons.ic_headphones_support_12,
        contentDescription = null,
    )
}