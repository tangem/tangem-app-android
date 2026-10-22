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

private var _ic_atm_20: ImageVector? = null

val Icons.ic_atm_20: ImageVector
    get() {
        if (_ic_atm_20 != null) return _ic_atm_20!!
        _ic_atm_20 = ImageVector.Builder(
            name = "ic_atm_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.041 11.7253C11.4549 11.7256 11.7909 12.0614 11.791 12.4753V13.7205C11.7908 14.1343 11.4548 14.4702 11.041 14.4705C10.6269 14.4705 10.2912 14.1345 10.291 13.7205V12.4753C10.2911 12.0612 10.6269 11.7253 11.041 11.7253Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.8613 3.49976C16.024 3.49989 16.9998 4.42244 17 5.60229V8.30835C17 9.48839 16.0241 10.4117 14.8613 10.4119H14.2217V14.3972C14.2215 15.5772 13.2458 16.4997 12.083 16.4998H7.91602C6.75339 16.4995 5.77749 15.5771 5.77734 14.3972V10.4119H5.13867C3.97586 10.4118 3 9.48841 3 8.30835V5.60229C3.00022 4.42243 3.97599 3.49987 5.13867 3.49976H14.8613ZM9.36133 14.9998H12.083C12.4546 14.9997 12.7215 14.7119 12.7217 14.3972V7.70581H9.36133V14.9998ZM7.27734 14.3972C7.27748 14.6961 7.51881 14.9669 7.86133 14.9939V7.70581H7.27734V14.3972ZM5.13867 4.99976C4.76713 4.99987 4.50022 5.28765 4.5 5.60229V8.30835C4.5 8.62315 4.76698 8.91176 5.13867 8.91187H5.77734V6.95581C5.77748 6.54179 6.11332 6.20594 6.52734 6.20581H13.4717C13.8858 6.20581 14.2215 6.54171 14.2217 6.95581V8.91187H14.8613C15.233 8.91173 15.5 8.62314 15.5 8.30835V5.60229C15.4998 5.28766 15.2328 4.99989 14.8613 4.99976H5.13867Z"),
            )
        }.build()
        return _ic_atm_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtm20Preview() {
    Icon(
        imageVector = Icons.ic_atm_20,
        contentDescription = null,
    )
}