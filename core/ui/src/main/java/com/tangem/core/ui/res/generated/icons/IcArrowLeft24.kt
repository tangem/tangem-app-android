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

private var _ic_arrow_left_24: ImageVector? = null

val Icons.ic_arrow_left_24: ImageVector
    get() {
        if (_ic_arrow_left_24 != null) return _ic_arrow_left_24!!
        _ic_arrow_left_24 = ImageVector.Builder(
            name = "ic_arrow_left_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.29199 4.29265C9.68241 3.90259 10.3156 3.90264 10.7061 4.29265C11.0962 4.68308 11.0962 5.31624 10.7061 5.70671L5.41406 10.9987H20.999C21.551 10.9988 21.9987 11.4467 21.999 11.9987C21.9988 12.5507 21.5511 12.9986 20.999 12.9987H5.41406L10.7061 18.2907C11.0962 18.6811 11.0962 19.3143 10.7061 19.7048C10.3156 20.0949 9.68242 20.0949 9.29199 19.7048L2.29297 12.7057C2.10577 12.5183 2.00009 12.2636 2 11.9987C2.00014 11.7338 2.10575 11.479 2.29297 11.2917L9.29199 4.29265Z"),
            )
        }.build()
        return _ic_arrow_left_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowLeft24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_left_24,
        contentDescription = null,
    )
}