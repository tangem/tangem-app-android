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

private var _ic_shield_checkmark_20: ImageVector? = null

val Icons.ic_shield_checkmark_20: ImageVector
    get() {
        if (_ic_shield_checkmark_20 != null) return _ic_shield_checkmark_20!!
        _ic_shield_checkmark_20 = ImageVector.Builder(
            name = "ic_shield_checkmark_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.5312 8.41447C11.8547 8.15571 12.3272 8.20821 12.5859 8.53166C12.8445 8.8551 12.7921 9.32765 12.4688 9.58634L9.96875 11.5863C9.69491 11.8053 9.30509 11.8053 9.03125 11.5863L7.53125 10.3861C7.20797 10.1274 7.15555 9.65487 7.41406 9.33146C7.67281 9.00815 8.14535 8.95563 8.46875 9.21427L9.5 10.0385L11.5312 8.41447Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.34961 3.24455C9.40583 2.78667 10.5952 2.78663 11.6514 3.24455L11.6504 3.24552L14.6504 4.54435C15.6833 4.99262 16.2997 6.02061 16.2998 7.09611V9.36271C16.2994 12.9044 13.8113 16.2355 10.3477 17.06C10.1196 17.1142 9.88142 17.1141 9.65332 17.06C6.18954 16.2356 3.70057 12.9044 3.7002 9.36271V7.09611C3.70034 6.02174 4.31493 4.9912 5.35059 4.54337L8.34961 3.24455ZM11.0547 4.62052C10.3792 4.32765 9.62183 4.32772 8.94629 4.62052L5.94629 5.92033C5.49527 6.11537 5.20034 6.58039 5.2002 7.09611V9.36271C5.20058 12.2475 7.24813 14.9459 10 15.601C12.7519 14.946 14.7994 12.2476 14.7998 9.36271V7.09611C14.7997 6.5806 14.5054 6.11614 14.0547 5.92033L11.0547 4.62052Z"),
            )
        }.build()
        return _ic_shield_checkmark_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcShieldCheckmark20Preview() {
    Icon(
        imageVector = Icons.ic_shield_checkmark_20,
        contentDescription = null,
    )
}