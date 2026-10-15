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

private var _ic_atm_12: ImageVector? = null

val Icons.ic_atm_12: ImageVector
    get() {
        if (_ic_atm_12 != null) return _ic_atm_12!!
        _ic_atm_12 = ImageVector.Builder(
            name = "ic_atm_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.66602 7.22266C6.94196 7.22289 7.16602 7.44666 7.16602 7.72266V8.58887C7.16557 8.86449 6.94169 9.08863 6.66602 9.08887C6.3902 9.0888 6.16646 8.86459 6.16602 8.58887V7.72266C6.16602 7.44655 6.38993 7.22272 6.66602 7.22266Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.11133 1.5C9.90501 1.50012 10.5 2.17265 10.5 2.94141V4.82324C10.5 5.592 9.90501 6.26453 9.11133 6.26465H8.72168V9.05957C8.7214 9.82814 8.12656 10.4999 7.33301 10.5H4.66602C3.87264 10.4997 3.27763 9.82802 3.27734 9.05957V6.26465H2.88867C2.09499 6.26453 1.5 5.592 1.5 4.82324V2.94141C1.5 2.17265 2.09499 1.50012 2.88867 1.5H9.11133ZM5.61133 9.5H7.33301C7.52128 9.49993 7.72141 9.33037 7.72168 9.05957V4.38281H5.61133V9.5ZM4.27734 9.05957C4.27759 9.3039 4.44117 9.46384 4.61133 9.49316V4.38281H4.27734V9.05957ZM2.88867 2.5C2.70031 2.50013 2.5 2.67034 2.5 2.94141V4.82324C2.5 5.0943 2.70031 5.26452 2.88867 5.26465H3.27734V3.88281C3.27734 3.60675 3.50131 3.38294 3.77734 3.38281H8.22168C8.49782 3.38281 8.72168 3.60667 8.72168 3.88281V5.26465H9.11133C9.29969 5.26452 9.5 5.0943 9.5 4.82324V2.94141C9.5 2.67034 9.29969 2.50013 9.11133 2.5H2.88867Z"),
            )
        }.build()
        return _ic_atm_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtm12Preview() {
    Icon(
        imageVector = Icons.ic_atm_12,
        contentDescription = null,
    )
}