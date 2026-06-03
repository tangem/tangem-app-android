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

private var _ic_arrow_down_24: ImageVector? = null

val Icons.ic_arrow_down_24: ImageVector
    get() {
        if (_ic_arrow_down_24 != null) return _ic_arrow_down_24!!
        _ic_arrow_down_24 = ImageVector.Builder(
            name = "ic_arrow_down_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.29244 14.707C3.90252 14.3167 3.90261 13.6834 4.29244 13.293C4.68283 12.9027 5.31598 12.9029 5.7065 13.293L10.9985 18.585L10.9985 3C10.9986 2.44808 11.4466 2.00042 11.9985 2C12.5507 2.00003 12.9984 2.44784 12.9985 3L12.9985 18.585L18.2905 13.293C18.6809 12.9027 19.314 12.9029 19.7045 13.293C20.0949 13.6835 20.0949 14.3165 19.7045 14.707L12.7055 21.7061C12.5181 21.8932 12.2634 21.999 11.9985 21.999C11.7336 21.9988 11.4788 21.8933 11.2915 21.7061L4.29244 14.707Z"),
            )
        }.build()
        return _ic_arrow_down_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_24,
        contentDescription = null,
    )
}