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

private var _ic_cross_24: ImageVector? = null

val Icons.ic_cross_24: ImageVector
    get() {
        if (_ic_cross_24 != null) return _ic_cross_24!!
        _ic_cross_24 = ImageVector.Builder(
            name = "ic_cross_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.2918 4.29436C18.6823 3.90383 19.3153 3.90383 19.7059 4.29436C20.0959 4.68492 20.0962 5.31804 19.7059 5.70842L13.4139 12.0004L19.7059 18.2924C20.0959 18.6829 20.0962 19.3161 19.7059 19.7065C19.3155 20.0966 18.6823 20.0965 18.2918 19.7065L11.9998 13.4145L5.70879 19.7065C5.31845 20.0968 4.68529 20.0964 4.29472 19.7065C3.90428 19.3159 3.90423 18.6829 4.29472 18.2924L10.5867 12.0004L4.29472 5.70842C3.9042 5.3179 3.9042 4.68488 4.29472 4.29436C4.68525 3.90385 5.31826 3.90384 5.70879 4.29436L11.9998 10.5854L18.2918 4.29436Z"),
            )
        }.build()
        return _ic_cross_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCross24Preview() {
    Icon(
        imageVector = Icons.ic_cross_24,
        contentDescription = null,
    )
}