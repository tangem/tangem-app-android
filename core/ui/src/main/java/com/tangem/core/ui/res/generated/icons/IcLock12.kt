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

private var _ic_lock_12: ImageVector? = null

val Icons.ic_lock_12: ImageVector
    get() {
        if (_ic_lock_12 != null) return _ic_lock_12!!
        _ic_lock_12 = ImageVector.Builder(
            name = "ic_lock_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 1.49976C7.23946 1.50003 8.21462 2.53683 8.21484 3.7771V4.17065C8.94596 4.20936 9.49989 4.82933 9.5 5.55542V9.11108C9.5 9.86104 8.90928 10.4998 8.14258 10.4998H3.85742C3.09072 10.4998 2.5 9.86104 2.5 9.11108V5.55542C2.50011 4.82901 3.05454 4.20886 3.78613 4.17065V3.7771C3.78636 2.53681 4.7605 1.49999 6 1.49976ZM3.85742 5.16675C3.67703 5.16675 3.50011 5.32328 3.5 5.55542V9.11108C3.5 9.34335 3.67698 9.49976 3.85742 9.49976H8.14258C8.32302 9.49976 8.5 9.34335 8.5 9.11108V5.55542C8.49989 5.32328 8.32297 5.16675 8.14258 5.16675H3.85742ZM6 2.49976C5.34672 2.49999 4.78636 3.05454 4.78613 3.7771V4.16675H7.21484V3.7771C7.21462 3.05456 6.65325 2.50003 6 2.49976Z"),
            )
        }.build()
        return _ic_lock_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLock12Preview() {
    Icon(
        imageVector = Icons.ic_lock_12,
        contentDescription = null,
    )
}