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

private var _ic_unlock_12: ImageVector? = null

val Icons.ic_unlock_12: ImageVector
    get() {
        if (_ic_unlock_12 != null) return _ic_unlock_12!!
        _ic_unlock_12 = ImageVector.Builder(
            name = "ic_unlock_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 1.5C7.21426 1.50026 8.21484 2.47018 8.21484 3.68457V4.4502C8.9234 4.48722 9.49984 5.06402 9.5 5.78906V9.1582C9.49977 9.90772 8.88378 10.5 8.14258 10.5H3.85742C3.11622 10.5 2.50023 9.90772 2.5 9.1582V5.78906C2.50017 5.0395 3.11619 4.44727 3.85742 4.44727H7.21484V3.68457C7.21484 3.03907 6.67872 2.50026 6 2.5C5.4289 2.50005 4.95607 2.88321 4.82324 3.38965C4.75329 3.65662 4.47987 3.81676 4.21289 3.74707C3.94592 3.67703 3.7866 3.40373 3.85645 3.13672C4.10438 2.19066 4.97388 1.50005 6 1.5ZM3.85742 5.44727C3.65173 5.44727 3.50017 5.60837 3.5 5.78906V9.1582C3.50023 9.33885 3.65177 9.5 3.85742 9.5H8.14258C8.34823 9.5 8.49977 9.33885 8.5 9.1582V5.78906C8.49983 5.60837 8.34827 5.44727 8.14258 5.44727H3.85742Z"),
            )
        }.build()
        return _ic_unlock_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUnlock12Preview() {
    Icon(
        imageVector = Icons.ic_unlock_12,
        contentDescription = null,
    )
}