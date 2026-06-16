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

private var _ic_chevron_right_24: ImageVector? = null

val Icons.ic_chevron_right_24: ImageVector
    get() {
        if (_ic_chevron_right_24 != null) return _ic_chevron_right_24!!
        _ic_chevron_right_24 = ImageVector.Builder(
            name = "ic_chevron_right_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.7929 4.29313C9.18345 3.90307 9.81659 3.90281 10.207 4.29313L17.206 11.2922C17.3933 11.4796 17.4988 11.7342 17.499 11.9992C17.4989 12.2641 17.3932 12.5188 17.206 12.7062L10.207 19.7052C9.81652 20.0956 9.18343 20.0955 8.7929 19.7052C8.40245 19.3147 8.40244 18.6817 8.7929 18.2912L15.0849 11.9992L8.7929 5.70719C8.40247 5.31671 8.40252 4.68365 8.7929 4.29313Z"),
            )
        }.build()
        return _ic_chevron_right_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronRight24Preview() {
    Icon(
        imageVector = Icons.ic_chevron_right_24,
        contentDescription = null,
    )
}