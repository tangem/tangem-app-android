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

private var _ic_filter_12: ImageVector? = null

val Icons.ic_filter_12: ImageVector
    get() {
        if (_ic_filter_12 != null) return _ic_filter_12!!
        _ic_filter_12 = ImageVector.Builder(
            name = "ic_filter_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.69434 6.34607C5.65951 6.34607 6.46386 7.01396 6.68652 7.91052H10C10.2761 7.91052 10.4999 8.13449 10.5 8.41052C10.5 8.68667 10.2761 8.91052 10 8.91052H6.68652C6.46402 9.80731 5.65966 10.475 4.69434 10.475C3.72904 10.4749 2.92463 9.8073 2.70215 8.91052H2C1.72386 8.91052 1.5 8.68667 1.5 8.41052C1.50012 8.13449 1.72393 7.91052 2 7.91052H2.70215C2.92478 7.01397 3.7292 6.34612 4.69434 6.34607ZM4.69434 7.34607C4.11411 7.34613 3.63879 7.82027 3.63867 8.41052C3.63867 9.00088 4.11404 9.47492 4.69434 9.47498C5.27468 9.47498 5.75 9.00092 5.75 8.41052C5.74988 7.82023 5.27461 7.34607 4.69434 7.34607ZM7.19434 1.47498C8.15965 1.47498 8.96401 2.14266 9.18652 3.03943H10C10.2761 3.03943 10.5 3.26329 10.5 3.53943C10.4999 3.81549 10.2761 4.03943 10 4.03943H9.18652C8.96387 4.936 8.15951 5.60388 7.19434 5.60388C6.22919 5.60383 5.42477 4.93599 5.20215 4.03943H2C1.72392 4.03943 1.5001 3.81548 1.5 3.53943C1.5 3.26329 1.72386 3.03943 2 3.03943H5.20215C5.42464 2.14266 6.22905 1.47503 7.19434 1.47498ZM7.19434 2.47498C6.61404 2.47503 6.13867 2.94907 6.13867 3.53943C6.13878 4.12969 6.61411 4.60382 7.19434 4.60388C7.77461 4.60388 8.24989 4.12973 8.25 3.53943C8.25 2.94903 7.77468 2.47498 7.19434 2.47498Z"),
            )
        }.build()
        return _ic_filter_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcFilter12Preview() {
    Icon(
        imageVector = Icons.ic_filter_12,
        contentDescription = null,
    )
}