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

private var _ic_trash_bin_12: ImageVector? = null

val Icons.ic_trash_bin_12: ImageVector
    get() {
        if (_ic_trash_bin_12 != null) return _ic_trash_bin_12!!
        _ic_trash_bin_12 = ImageVector.Builder(
            name = "ic_trash_bin_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.97266 1C7.72075 1.00051 8.32402 1.60894 8.32422 2.35449V2.70215H9.87891C10.224 2.70215 10.5038 2.98209 10.5039 3.32715C10.5039 3.67232 10.2241 3.95215 9.87891 3.95215H9.77832V9.40527C9.77832 10.2855 9.06597 11.0028 8.18359 11.0029H3.82227C2.93977 11.0029 2.22754 10.2856 2.22754 9.40527V3.95215H2.125C1.77994 3.95202 1.50001 3.67224 1.5 3.32715C1.50014 2.98217 1.78002 2.70228 2.125 2.70215H3.68164V2.35449C3.68184 1.60876 4.28486 1.00022 5.0332 1H6.97266ZM3.47754 9.40527C3.47754 9.59922 3.63411 9.75293 3.82227 9.75293H8.18359C8.37164 9.7528 8.52832 9.59914 8.52832 9.40527V3.95215H3.47754V9.40527ZM5.0332 2.25C4.97916 2.25022 4.93184 2.29516 4.93164 2.35449V2.70215H7.07422V2.35449C7.07402 2.29536 7.02648 2.25052 6.97266 2.25H5.0332Z"),
            )
        }.build()
        return _ic_trash_bin_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTrashBin12Preview() {
    Icon(
        imageVector = Icons.ic_trash_bin_12,
        contentDescription = null,
    )
}